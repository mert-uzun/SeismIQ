import os, csv, time, requests
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo         


api_key = os.getenv("twitterapi_io_key")
base_url = "https://api.twitterapi.io/twitter/tweet/advanced_search"

#Latest is used to go chronologically
#Top can also be used to see the most engaged Tweets
query_type = "Latest"

# StartS from UTC +03:00 and moves forward in slices of 10 minutes
start_local = "2025-08-10 19:53:46"   
end_local   = None                    
slice_minutes = 10                    
tweet_cap_total = 65005
#Optionally can set up a cap based on credits                

#Filter only shows #deprem, Turkish, no media, no rt, no replies:
#lang_filter = "lang:tr -filter:media -filter:retweets -filter:replies"

#Filter only shows #deprem, Turkish, shows media no rt, no replies:
lang_filter = "lang:tr filter:media -filter:retweets -filter:replies"

out_csv = "Any file you want to save data to.csv"

fields = [
    "id","url","created_at","author_username","author_name","author_followers",
    "text","hashtags","lang","source","retweet_count","reply_count","like_count",
    "quote_count","view_count","bookmark_count","is_reply","conversation_id"
]

istanbul = ZoneInfo("Europe/Istanbul")

def to_utc_str(dt_local):
    dt = dt_local.replace(tzinfo=istanbul)
    return dt.astimezone(ZoneInfo("UTC")).strftime("%Y-%m-%d_%H:%M:%S_UTC")

def to_tr_str(created_at_str):
    try:
        dt_utc = datetime.strptime(created_at_str, "%a %b %d %H:%M:%S %z %Y")
        return dt_utc.astimezone(istanbul).strftime("%Y-%m-%d %H:%M:%S %z")
    except Exception:
        return created_at_str

def build_query(since_utc, until_utc):
    base_terms = "#deprem"
    parts = [base_terms, f"since:{since_utc}"]
    if until_utc:
        parts.append(f"until:{until_utc}")
    parts.append(lang_filter)
    return " ".join(parts)

def extract_hashtags_list(t):
    ents = t.get("entities") or {}
    tags = ents.get("hashtags") or []
    return [h.get("text") for h in tags if h and h.get("text")]

def row_from_tweet(t):
    a = t.get("author") or {}
    hashtags_list = extract_hashtags_list(t)
    return {
        "id": t.get("id"),
        "url": t.get("url"),
        "created_at": to_tr_str(t.get("createdAt")), 
        "author_username": a.get("userName"),
        "author_name": a.get("name"),
        "author_followers": a.get("followers"),
        "text": (t.get("text") or "").replace("\r"," ").replace("\n"," ").strip(),
        "hashtags": str(hashtags_list),
        "lang": t.get("lang"),
        "source": t.get("source"),
        "retweet_count": t.get("retweetCount"),
        "reply_count": t.get("replyCount"),
        "like_count": t.get("likeCount"),
        "quote_count": t.get("quoteCount"),
        "view_count": t.get("viewCount") or t.get("ViewCount"),
        "bookmark_count": t.get("bookmarkCount"),
        "is_reply": t.get("isReply"),
        "conversation_id": t.get("conversationId"),
    }

def get_page(headers, params):
    """Request one page; honor Retry-After on 429 and back off on 5xx."""
    backoff = 1.0
    while True:
        r = requests.get(base_url, headers=headers, params=params, timeout=30)

        if r.status_code == 429:
            ra = r.headers.get("Retry-After")
            try:
                sleep_s = int(float(ra)) if ra else 10
            except ValueError:
                sleep_s = 10
            print(f"rate limit (429). sleeping {sleep_s}s...")
            time.sleep(sleep_s)
            continue 

        if 500 <= r.status_code < 600:
            print(f"http {r.status_code}. retry in {int(backoff)}s...")
            time.sleep(backoff)
            backoff = min(60, backoff * 2)
            continue

        r.raise_for_status()
        return r.json()

def fetch_forward():
    if not api_key or api_key == "YOUR_KEY":
        raise SystemExit("set env twitterapi_io_key or paste your key into api_key")

    headers = {"X-API-Key": api_key.strip()}
    start_dt = datetime.strptime(start_local, "%Y-%m-%d %H:%M:%S")
    end_dt = datetime.strptime(end_local, "%Y-%m-%d %H:%M:%S") if end_local else None

    seen = set()
    total = 0

    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields, quoting=csv.QUOTE_MINIMAL)
        w.writeheader()

        current_start = start_dt
        while True:
            current_end = current_start + timedelta(minutes=slice_minutes)
            if end_dt and current_end > end_dt:
                current_end = end_dt

            since_utc = to_utc_str(current_start)
            until_utc = to_utc_str(current_end) if (end_dt or slice_minutes) else None
            q = build_query(since_utc, until_utc)

            print(f"\nwindow (TR): {current_start} → {current_end if end_dt or slice_minutes else 'open-ended'}")
            print(f"query: {q}")

            cursor = ""
            while True:
                params = {"query": q, "queryType": query_type}
                if cursor:
                    params["cursor"] = cursor

                data = get_page(headers, params)
                tweets = data.get("tweets") or []

                for t in tweets:
                    tid = t.get("id")
                    if not tid or tid in seen:
                        continue
                    w.writerow(row_from_tweet(t))
                    seen.add(tid)
                    total += 1
                    print(f"saved {total}: {tid}")

                    if tweet_cap_total and total >= tweet_cap_total:
                        print(f"\Cap reached. saved {total} to {out_csv}")
                        return

                if not data.get("has_next_page"):
                    break
                cursor = data.get("next_cursor") or ""
                if not cursor:
                    break
                time.sleep(0.4)

            if end_dt and current_end >= end_dt:
                break
            current_start = current_end

    print(f"\Saved {total} tweets to {out_csv}")

if __name__ == "__main__":
    fetch_forward()
