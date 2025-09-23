#!/usr/bin/env python
# coding: utf-8

# In[16]:


import re
import csv


# In[ ]:


import re
import csv

# regex patterns
hashtag_pat = re.compile(r"#\S+")
non_tr_pat = re.compile(r"[^a-zA-ZçÇğĞıİöÖşŞüÜÂâÎîÛû0-9\s]")
url_pat = re.compile(r"https?://\S+|www\.\S+")
all_letters = "A-Za-zÇĞİIÖŞÜçğıöşü"
tr_vowels = "AaEeIıİiOoÖöUuÜü"

# banned keywords/phrases
banned_keywords = [
    "suriyeli", "allah aşk", "allah ra", "allah yar", "allahtan",
    "uygulama", "vpn", "tl", "iban", "dolar", "euro",
    "rabbim", "ücretsiz", "ucretsiz", "hz", "orospu",
    "neredeyim", "büyüklü", "vur emr", "şükür", "şifalar",
    "maddi destek", "şiddet", "allah ım", "geçmiş olsun", "allahım", "inşallah"
    "turkcell","vodafone","turk telekom","turktelekom","allahim"
]

def remove_hashtags(text: str):
    return hashtag_pat.sub("", text).strip()

url_pattern = re.compile(r'https?://\S+|www\.\S+')

def remove_links(text: str) -> str:
    return url_pattern.sub('', text)

def to_lower(text: str):
    return text.lower()

def remove_non_unicode(text: str):
    return non_tr_pat.sub("", text)

def collapse_elongations(text: str, max_run: int = 2, vowels_only: bool = False):
    if max_run < 1:
        max_run = 1
    cls = tr_vowels if vowels_only else all_letters
    pattern = re.compile(rf"([{cls}])\1{{{max_run},}}", re.UNICODE)
    return pattern.sub(lambda m: m.group(1) * max_run, text)

def word_count_filter(text: str, min_words: int = 3) -> bool:
    return len(text.strip().split()) > min_words

def remove_keywords(text: str):
    lowered = text.lower()
    for kw in banned_keywords:
        if kw in lowered:
            return ""   # drop tweet completely
    return text

# zemberek + fallback analyzers
try:
    from zemberek.normalization import NoisyTextNormalizer
    z_normalizer = NoisyTextNormalizer()
except Exception:
    z_normalizer = None

z_morph = None
try:
    from zemberek.morphology import TurkishMorphology
    z_morph = TurkishMorphology()
except Exception:
    z_morph = None

z_zeyrek = None
try:
    import zeyrek
    z_zeyrek = zeyrek.MorphAnalyzer()
except Exception:
    z_zeyrek = None

def normalize_text(text: str):
    if z_normalizer is None:
        return text
    try:
        return z_normalizer.normalize(text)
    except Exception:
        return text

def lemmatize_text(text: str):
    tokens = text.split()
    if z_morph is not None:
        lemmas = []
        for tok in tokens:
            try:
                analyses = z_morph.analyze(tok)
                if analyses:
                    lemmas.append(analyses[0].lemma)
                else:
                    lemmas.append(tok)
            except Exception:
                lemmas.append(tok)
        return " ".join(lemmas)
    if z_zeyrek is not None:
        try:
            pairs = z_zeyrek.lemmatize(text)
            return " ".join((lemmas[0] if lemmas else w) for (w, lemmas) in pairs)
        except Exception:
            return text
    return text

def clean_tweet(text: str, use_normalizer: bool = True, min_words: int = 3):
    text = remove_links(text)
    text = remove_hashtags(text)
    text = to_lower(text)
    text = remove_non_unicode(text)
    text = collapse_elongations(text, max_run=2)

    if not word_count_filter(text, min_words=min_words):
        return ""

    if use_normalizer:
        text = normalize_text(text)

    text = lemmatize_text(text)
    text = re.sub(r"\s+", " ", text).strip()

    # drop tweet if banned keyword present
    text = remove_keywords(text)

    return text


# In[3]:


if __name__ == "__main__":
    input_csv = "Aug10_earthquake_tweets.csv"
    column_name = "text"

    with open(input_csv, mode="r", encoding="utf-8", newline="") as infile:
        reader = csv.DictReader(infile)
        shown = 0
        for row in reader:
            raw_text = row.get(column_name, "")
            if not raw_text:
                continue
            cleaned_text = clean_tweet(raw_text, use_normalizer=True, min_words=3)
            if cleaned_text:
                print(cleaned_text)
                shown += 1
            if shown >= 200:
                break

