import os

from dotenv import load_dotenv
from tweet_preprocessing import preprocess_tweet, realtime_tfidf_for_new_tweets
import tweepy
import datetime
import boto3
import time

load_dotenv()

# Twitter API setup
tw_api_key = os.getenv("TWITTER_API_KEY")
tw_api_secret = os.getenv("TWITTER_API_SECRET")
tw_access_token = os.getenv("TWITTER_ACCESS_TOKEN")
tw_access_token_secret = os.getenv("TWITTER_ACCESS_TOKEN_SECRET")
auth = tweepy.OAuth1UserHandler(tw_api_key, tw_api_secret, tw_access_token, tw_access_token_secret)
api = tweepy.API(auth)

# DynamoDB setup
dynamodb = boto3.resource("dynamodb")
tweets_table = dynamodb.Table(os.environ["TWEETS_TABLE_NAME"])
last_seen_table = dynamodb.Table(os.environ["LAST_SEEN_TABLE_NAME"])

def lambda_handler(event, context):
    
    # Get last seen ID
    last_seen_id = get_last_seen_id(last_seen_table)

    if(last_seen_id is None): # use time filter
        tweets = api.search_tweets(
            q="#deprem (#yardım OR ihtiyac OR yardım OR ihtiyaç OR enkaz OR erzak) lang:tr -is:retweet",
            count=100,
            tweet_mode="extended",
            result_type="recent"
        )

        # filter by time last 30 minutes because this is the first search
        tweets = [tweet for tweet in tweets if tweet.created_at > datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(minutes=30)]
    else: # use since_id
        tweets = api.search_tweets(
            q="#deprem (#yardım OR ihtiyac OR yardım OR ihtiyaç OR enkaz OR erzak) lang:tr -is:retweet",
            since_id=last_seen_id,
            count=100,
            tweet_mode="extended",
            result_type="recent"
        )
        
    # Process and store tweets
    tfidf_table = dynamodb.Table(os.environ["TFIDF_TABLE_NAME"])

    for tweet in tweets:
        tweet_id = tweet.id_str
        text = tweet.full_text
        created_at = tweet.created_at
        user = tweet.user.screen_name
        hashtags = [hashtag["text"] for hashtag in tweet.entities.get("hashtags", [])]
        ten_years_from_now = int(time.time()) + 10 * 365 * 24 * 60 * 60

        tweets_table.put_item(Item={
            "tweet_id": tweet_id,
            "text": text,
            "processed_data": preprocess_tweet(text),
            "created_at": str(created_at),
            "user": user,
            "hashtags": hashtags,
            "ttl": ten_years_from_now # Delete the data after 10 years of its entry
        })

        realtime_tfidf_for_new_tweets(tweets_table, tfidf_table, tweet_id, 15)

    # Update since_id
    if tweets:
        update_last_seen_id(last_seen_table, tweets[0].id_str) # since_id is the id of the newest tweet system got from the last search

    return {"statusCode": 200, "body": "Success"}

def get_last_seen_id(table: boto3.resource("dynamodb").Table) -> str:
    response = table.get_item(Key={"tracker_id": "since_id"})
    last_seen_id = response.get("Item", {}).get("value")
    return str(last_seen_id) if last_seen_id else None

def update_last_seen_id(table: boto3.resource("dynamodb").Table, value: str):
    table.put_item(Item={"tracker_id": "since_id", "value": value})

def get_hashtags(text: str) -> list[str]:
    hashtag_indices = [i for i, char in enumerate(text) if char == '#']
    hashtags = []

    for i in hashtag_indices:
        space_index = text.find(" ", i)
        if space_index == -1:
            hashtags.append(text[i:])
        else:
            hashtags.append(text[i:space_index])
    
    return hashtags