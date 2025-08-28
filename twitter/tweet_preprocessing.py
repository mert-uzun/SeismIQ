import re
import string
import jpype
import boto3
from twitter_scraper import get_hashtags
import jpype.imports
from jpype.types import JString
import spacy
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from Tweet_preprocessingv2 import collapse_elongations, word_count_filter

# =============================== CLEANING =============================== #
banned_keywords = [
    "suriyeli", "allah aşk", "allah ra", "allah yar", "allahtan",
    "uygulama", "vpn", "tl", "iban", "dolar", "euro",
    "rabbim", "ücretsiz", "ucretsiz", "hz", "orospu",
    "neredeyim", "büyüklü", "vur emr", "şükür", "şifalar",
    "maddi destek", "şiddet", "allah ım", "geçmiş olsun", "allahım", "inşallah"
    "turkcell","vodafone","turk telekom","turktelekom","allahim"
]

def remove_mentions(text: str) -> str:
    return re.sub(r"@\w+", "", text)

def remove_urls(text: str) -> str:
    return re.sub(r"http[s]?://\S+", "", text)

def remove_emojis(text: str) -> str:
    emoji_pattern = re.compile("["
    u"\U0001F600-\U0001F64F"  # Emoticons
    u"\U0001F300-\U0001F5FF"  # Symbols & pictographs
    u"\U0001F680-\U0001F6FF"  # Transport & map symbols
    u"\U0001F1E0-\U0001F1FF"  # Flags
    u"\U00002700-\U000027BF"  # Dingbats
    u"\U0001F900-\U0001F9FF"  # Supplemental Symbols and Pictographs
    u"\U00002600-\U000026FF"  # Misc symbols
    u"\U00002B00-\U00002BFF"  # Arrows etc.
    u"\U0001FA70-\U0001FAFF"  # Extended emoji
    u"\U000025A0-\U000025FF"  # Geometric Shapes
    "]+", flags=re.UNICODE)

    return emoji_pattern.sub("", text)

def remove_hastags(text: str) -> str:


    hashtags = get_hashtags(text)
    for hashtag in hashtags:
        text = text.replace(hashtag, "")

    return text

def keep_alphanumeric_and_turkish(text: str) -> str:
    return re.sub(r"[^a-zA-Z0-9ğüşıöçĞÜŞİÖÇ\s]", "", text)

def lower_case(text: str) -> str:
    return text.lower()

def clean_whitespace(text: str) -> str:
    text = re.sub(r"\s+", " ", text) # Replace multiple spaces with a single space
    return text.strip()

def remove_punctuation(text: str) -> str:
    return "".join(char for char in text if char not in string.punctuation)

def clean_tweet(text: str, min_words: int = 3) -> str:
    if not text or not isinstance(text, str):
        return ""

    text = remove_mentions(text)
    text = remove_urls(text)
    text = remove_hastags(text)
    text = keep_alphanumeric_and_turkish(text)
    text = remove_emojis(text)
    text = lower_case(text)
    text = clean_whitespace(text)
    text = drop_banned_words(text)
    text = collapse_elongations(text, max_run=2)
    text = word_count_filter(text, min_words=min_words)

    return text

def drop_banned_words(text: str):
    if banned_keywords is None:
        return text
    try:
        if any(word in banned_keywords for word in text.split("")):
            return ""
        return text
    except Exception:
        return text

# ============================= LEMMATIZATION ============================= #
ZEMBEREK_PATH = "/opt/zemberek-full.jar"

# Start the JVM if it's not already started
if not jpype.isJVMStarted():
    jpype.startJVM(classpath=[ZEMBEREK_PATH])

# Import TurkishMorphology from zemberek-full.jar after starting the JVM, this will cause a linter warning but it's fine
try:
    from zemberek.morphology import TurkishMorphology # type: ignore
    morphology = TurkishMorphology.createWithDefaults()
except Exception:
    morphology = None

try:
    from zemberek.normalization import NoisyTextNormalizer # type: ignore
    normalizer = NoisyTextNormalizer()
except Exception:
    normalizer = None

def normalize_text(text: str) -> str:
    if normalizer is None:
        return text
    try:
        return normalizer.normalize(text)
    except Exception:
        return text

def lemmatize(text: str) -> str:
    if morphology is None:
        return text
    
    results = []

    for word in text.split(" "):
        try:
            analysis = morphology.analyzeSentence(JString(word))
            lemmas = [str(s.getLemmas()[0]) for s in analysis]
            results.append(lemmas[0] if lemmas else word)
        except Exception:
            results.append(word)
    
    
    return " ".join(results)

# ========================= STOP WORD EXTRACTION ========================= #
turkish_stopwords: list[str] = [
    "acaba", "ama", "aslında", "az", "bazı", "belki", "biri", "birkaç", "birşey", "biz", "bu",
    "çok", "çünkü", "da", "daha", "de", "defa", "diye", "en", "gibi", "hem", "hep", "hepsi", "her",
    "hiç", "için", "ile", "ise", "kez", "ki", "kim", "mı", "mi", "mu", "mü", "nasıl", "ne", "neden",
    "nerde", "nerede", "nereye", "niçin", "niye", "o", "sanki", "şayet", "şey", "siz", "şu", "tüm",
    "ve", "veya", "ya", "yani", "bizim", "ben", "sen", "siz", "onlar", "bizler", "sizler", "kendi",
    "kendine", "kendisini", "benim", "senin", "onun", "bizim", "sizin", "onların", "herkes",
    "herhangi", "herhalde", "o", "şöyle", "böyle", "şimdi", "dün", "bugün", "yarın", "evet",
    "hayır", "değil", "ile", "ancak", "göre", "üzere", "üzere", "yüzünden", "sebebiyle",
    "dolayı", "başka", "arada", "artık", "beri", "bir", "iki", "üç", "dört", "beş", "altı",
    "yedi", "sekiz", "dokuz", "on", "ama", "fakat", "lakin", "yalnız", "ya", "hem", "hem de",
    "hatta", "ile", "ve", "veyahut", "çünkü", "zira", "madem", "oysa", "oysaki", "halbuki",
    "meğer", "meğerse", "varsayalım", "diyelim", "birden", "bile", "ile", "üzere", "olarak",
    "karşı", "kadar", "göre", "yaklaşık", "üzeri", "alttan", "arasında", "dahil", "hariç",
    "bile", "dahi", "keza", "nitekim", "özellikle", "mesela", "örneğin", "yani", "kısaca",
    "öz", "diğer", "aynı", "biraz", "çoğu", "hemen", "kesin", "sadece", "yalnızca",
    "zaten", "birlikte", "üzerinde", "bununla", "şununla", "onunla", "herkesle", "hepimiz",
    "kimseyle", "herhangi", "hiçbir", "bazıları", "çoğu", "birçoğu", "bazısı", "hangi",
    "bütün", "tümü", "tamamı"
]

def remove_stopwods(text: str) -> str:
    return " ".join([word for word in text.split(" ") if word not in turkish_stopwords])

# ============================= TOKENIZATION ============================= #
nlp = spacy.load("xx_ent_wiki_sm")

def tokenize(text: str) -> list[str]:
    doc = nlp(text)
    return [token.text for token in doc]

def spacy_ner(text: str) -> list[str]:
    doc = nlp(text)
    return [(ent.text, ent.label_) for ent in doc.ents]

# =============================== PIPELINE =============================== #
def preprocess_tweet(tweet_raw_content: str) -> dict:
    clean_text = clean_tweet(tweet_raw_content)
    normalized = normalize_text(clean_text)
    lemmatized = lemmatize(normalized)
    stopwords_removed = remove_stopwods(lemmatized)
    tokens = tokenize(stopwords_removed)    
    locations_ner = extract_locations(stopwords_removed)
    features = handcrafted_features(clean_text, tokens)

    return {
        "clean_text": clean_text,
        "lemmatized": lemmatized,
        "stopwords_removed": stopwords_removed,
        "tokens": tokens,
        "locations_ner": locations_ner,
        "features": features,
        "raw_content": tweet_raw_content
    }

# ========================== FEATURE EXTRACTION ========================== #

# TF-IDF
def save_tfidf_model(model_table, model_id, vectorizer):
    model_table.put_item(Item={
        "model_id": model_id,
        "vocabulary": dict(vectorizer.vocabulary_),
        "idf_weights": [str(float(weight)) for weight in vectorizer.idf_],
        "ngram_range": vectorizer.ngram_range
    })

def load_tfidf_model(model_table, model_id):
    response = model_table.get_item(Key={"model_id": model_id})
    item = response.get("Item", {})

    if not item:
        raise Exception(f"Model with ID {model_id} not found")
    
    vectorizer = TfidfVectorizer(ngram_range=item["ngram_range"])
    vectorizer.vocabulary_ = {k: int(v) for k, v in item["vocabulary"].items()}
    vectorizer.idf_ = np.array([float(weight) for weight in item["idf_weights"]])

    return vectorizer

def _get_clean_text(item: dict) -> str:
    if "stopwords_removed" in item:
        return item["stopwords_removed"]

    return preprocess_tweet(item.get("text", "")).get("stopwords_removed", "")

def _get_top_k_tfidf(tfidf_row, feature_names, top_k):
    if tfidf_row.nnz == 0:
        return {}

    coo = tfidf_row.tocoo()

    term_scores = [(i, score) for i, score in zip(coo.col, coo.data)]
    term_scores.sort(key=lambda x: x[1],reverse=True)

    return {feature_names[i]: score for (i, score) in term_scores[:top_k]}

def batch_fit_and_write_tfidf(source_table, model_table):
    # 1. Read all tweets from DynamoDB
    response = source_table.scan()
    tweets = response.get("Items", [])

    # 2. Extract text and preprocess
    clean_contents = []
    for tweet in tweets:
        if "processed_data" in tweet:
            clean_content = tweet.get("processed_data", {}).get("stopwords_removed", "")
        else:
            clean_content = preprocess_tweet(tweet.get("text", "")).get("stopwords_removed", "")
        
        if clean_content.strip():
            clean_contents.append(clean_content)

    # 3. Fit TF-IDF model on all texts
    vectorizer = TfidfVectorizer(ngram_range=(1, 2))
    tfidf_matrix = vectorizer.fit_transform(clean_contents)
    words_in_tweets = vectorizer.get_feature_names_out()

    # 4. Save model to model_table
    save_tfidf_model(model_table, "tfidf_model", vectorizer)

    # 5. Calculate TF-IDF for each tweet
    for i in range(tfidf_matrix.shape[0]):
        top_k_terms = _get_top_k_tfidf(tfidf_matrix[i], words_in_tweets, 15)
        tweet_id = tweets[i].get("tweet_id", "")
        if tweet_id:
            source_table.update_item(
                Key={"tweet_id": tweet_id},
                UpdateExpression="SET tfidf_terms = :tfidf_terms",
                ExpressionAttributeValues={":tfidf_terms": top_k_terms}
            )
        
    return {
        "status": "success",
        "exit_code": 0
    }

def realtime_tfidf_for_new_tweets(source_table, model_table, new_tweet_id: str, top_k: int):
    try:    
        # 1. Load saved model
        vectorizer = load_tfidf_model(model_table, "tfidf_model")
        # 2. Get new tweet from DynamoDB
        response = source_table.get_item(Key={"tweet_id": new_tweet_id})
        # 3. Preprocess tweet text
        clean_content = response.get("Item", {}).get("processed_data", {}).get("stopwords_removed", "")
        # 4. Calculate TF-IDF using saved model
        tfidf_row = vectorizer.transform([clean_content])
        # 5. Write top-K scores back to new tweet
        top_k_terms = _get_top_k_tfidf(tfidf_row[0,:], vectorizer.get_feature_names_out(), top_k)

        source_table.update_item(
            Key={"tweet_id": new_tweet_id},
            UpdateExpression="SET tfidf_terms = :tfidf_terms",
            ExpressionAttributeValues={":tfidf_terms": top_k_terms}
        )

        return {
            "status": "success",
            "exit_code": 0
        }
    except Exception as e:
        return {
            "status": "error",
            "message": str(e),
            "exit_code": 1
        }

# NER via SpaCy
def extract_locations(text: str) -> list[str]:
    doc = nlp(text)
    return [ent.text for ent in doc.ents if ent.label_ == "LOC"]

# Negation detection
def detect_negation_lemma(cleaned_text: str) -> bool:
    analysis = morphology.analyzeSentence(JString(cleaned_text))
    
    return any("Neg" in s.getMorphemes() for s in analysis)
    
# Handcrafted Features
need_keywords = [
    "lazım", "gerekmek", "ihtiyaç", "şart", "zorunlu", "elzem", "gerek", "mecbur", 
    "gereksinim", "muhtaç", "eksik", "yok", "kıt", "eksiklik", "noksan", "yetmek", "yetersiz", "az",
    "bitmek", "tükenmek", "kalmamak", "hiç", "yoktan", "mahrum", "yoksun"
]

urgent_keywords = [
    "acil", "hemen", "ivedi", "acilen", "derhal", "şimdi", "an", "tez", "fori", "çabuk",
    "hızlı", "kritik", "hayati", "hayat", "ölüm", "can", "tehlike", "risk",
    "saat", "gün", "zaman", "beklemek"
]

help_keywords = [
    "yardım", "destek", "yardımcı", "desteklemek", "kurtarmak", "kurtarma", "çıkarmak",
    "götürmek", "getirmek", "göndermek", "ulaştırmak", "dağıtmak", "vermek", "sağlamak"
]

situation_keywords = [
    "sıkışmak", "mahsur", "enkaz", "alt", "iç", "yara", "hasta", "zor", "zorlu",
    "çaresiz", "çare", "imdat", "kurtulamamak", "çıkamak", "kapalı", "kapatmak", "kapanmak", "kesilmek"
]

negative_keywords = [
    "yok", "hiç", "yoktan", "bitmek", "tükenmek", "kesilmek", "kapanmak", "değil"]

location_indicators = ["bura", "şura", "ora", "yakın", "civar", "bölge", "burada", "burda", "şurada", "orada", "yakında", "civarda", "bölgede"]

time_indicators = ["saat", "gün", "dakika", "zaman", "saattir", "gündür", "dakikadır", "zamandır", "önce", "sonra"]

quantity_indicators = ["çok", "az", "bol", "yetmek", "yetersiz", "fazla", "kafi"]

def handcrafted_features(clean_text: str, tokens: list[str]) -> dict:
    features = {}
    features["length"] = len(clean_text)
    features["num_tokens"] = len(tokens)
    features["is_urgent"] = int(any(word in urgent_keywords for word in tokens))
    features["is_need"] = int(any(word in need_keywords for word in tokens))
    features["is_help"] = int(any(word in help_keywords for word in tokens))
    features["is_situation"] = int(any(word in situation_keywords for word in tokens))
    features["is_negative"] = int(any(word in negative_keywords for word in tokens))
    features["has_negation"] = int(detect_negation_lemma(clean_text))
    features["is_exclamation"] = int("!" in clean_text)
    features["is_question"] = int("?" in clean_text)
    features["exclamation_count"] = clean_text.count("!")
    features["question_count"] = clean_text.count("?")
    features["is_location_indicated"] = int(any(word in location_indicators for word in tokens))
    features["is_time_indicated"] = int(any(word in time_indicators for word in tokens))
    features["is_quantity_indicated"] = int(any(word in quantity_indicators for word in tokens))
    features["caps_ratio"] = sum(1 for c in clean_text if c.isupper()) / len(clean_text) if clean_text else 0
    features["has_caps"] = int(features["caps_ratio"] > 0.25)

    max_urgency_score = 2.5 + 5 + 3 + 4 + 4 + 1 + 2 + 2
    features["urgency_score"] = 2.5 * features["is_urgent"] + 5 * features["is_situation"] + 3 * features["is_need"] + 4 * features["is_help"] + 4 * features["is_time_indicated"] + features["is_exclamation"] + 2 * features["has_caps"] + 2 * features["has_negation"]
    features["urgency_score_normalized"] = features["urgency_score"] / max_urgency_score

    max_rescue_call_score = 4 + 2 + 3
    features["is_likely_rescue_call"] = 4 * features["is_situation"] + 2 * features["is_help"] + 3 * features["is_location_indicated"]
    features["rescue_call_score_normalized"] = features["is_likely_rescue_call"] / max_rescue_call_score

    max_supply_call_score = 4 + 2 + 2
    features["is_likely_supply_call"] = 4 * features["is_need"] + 2 * features["is_help"] + 2 * features["is_quantity_indicated"]
    features["supply_call_score_normalized"] = features["is_likely_supply_call"] / max_supply_call_score

    features["urgency_level"] = (
        "very_high" if features["urgency_score_normalized"] > 0.7 else
        "high" if features["urgency_score_normalized"] > 0.5 else
        "medium" if features["urgency_score_normalized"] > 0.3 else
        "low"
    )

    features["is_high_priority"] = int(
        features["urgency_score_normalized"] > 0.75 or 
        features["rescue_call_score_normalized"] > 0.75
        )

    if features["has_negation"]:
        features["is_negative"] = 1
        features["is_need"] = 0.8 if features["is_need"] == 0 else 1

    return features

# --------------------- GPT ----------------------- #

import json, os
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def extract_features_with_gpt(text: str) -> dict:
    prompt = """
        Analyze this Turkish tweet for earthquake disaster response. Extract emergency-related features and return ONLY a JSON object with these fields, without any explanations:

        {
        "emergency_type": one of ["medical_aid", "supply", "shelter", "rescue", "transport", "none"],
        "urgency_level": one of ["very_high", "high", "medium", "low"],
        "need_type": one of ["need_help", "offering_help", "information", "none"],
        "location_mentioned": boolean,
        "location_names": array of location names mentioned,
        "situation_severity": one of ["life_threatening", "serious", "moderate", "minor", "none"],
        "resource_type": array of mentioned resources,
        "victim_count": one of ["one", "few", "many", "unknown", "none"],
        "time_sensitivity": one of ["immediate", "hours", "days", "none"],
        "contact_info_present": boolean,
        "contact_info": identify if present
        "infrastructure_damage": boolean,
        "medical_emergency": boolean,
        "trapped_people": boolean,
        "supply_shortage": boolean,
        "communication_need": boolean
        }

        Tweet: "{tweet_text}"
    """
    