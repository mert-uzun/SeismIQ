import pytest
import re
from unittest.mock import Mock, MagicMock
import tweet_preprocessing2 as tp


class TestTextCleaningFunctions:

    def test_remove_hashtags(self):
        text = "bu bir #deneme #testtir ve önemli."
        expected = "bu bir   ve önemli."
        assert tp.remove_hashtags(text) == expected

    def test_remove_links(self):
        # Remove link
        text = "Bilgi: https://example.com/page?id=1 ve www.google.net/ara"
        expected = "Bilgi:  ve "
        assert tp.remove_links(text) == expected

    def test_remove_non_unicode(self):
        # Keep only Turkish alphabet letters, numbers, and spaces
        text = "Hey! Bu bir test: 123. Dolar ($) ne oldu?"
        expected = "Hey Bu bir test 123 Dolar  ne oldu"
        assert tp.remove_non_unicode(text) == expected

    def test_word_count_filter_pass(self):
        # Keep if more than 3 words
        text = "bu metin uzun yeterli"
        assert tp.word_count_filter(text, min_words=3) is True

    def test_word_count_filter_fail(self):
        # Exactly 3 words, should fail 
        text = "bu kısa metin"
        assert tp.word_count_filter(text, min_words=3) is False
        assert tp.word_count_filter("", min_words=3) is False

    def test_remove_keywords_drop(self):
        # Drops tweet when keyword found
        text = "acil maddi destek lazım buraya"
        assert tp.remove_keywords(text) == ""

    def test_remove_keywords_keep(self):
        # Keep tweet if no keyword found
        text = "acil yardım ve işbirliği gerekli"
        assert tp.remove_keywords(text) == text


class TestNLPFunctions:

    def test_normalize_text_no_normalizer(self, monkeypatch):
        # Test case where z_normalizer is not installed
        monkeypatch.setattr(tp, 'z_normalizer', None)
        text = "bu bir deneme metnidir"
        assert tp.normalize_text(text) == text

    def test_normalize_text_with_mock_normalizer(self, monkeypatch):
        # Test case where z_normalizer is mocked
        mock_normalizer = Mock()
        mock_normalizer.normalize.return_value = "normalized text"
        monkeypatch.setattr(tp, 'z_normalizer', mock_normalizer)
        
        text = "bu bır dnm mtnıdır"
        result = tp.normalize_text(text)
        
        mock_normalizer.normalize.assert_called_once_with(text)
        assert result == "normalized text"

    def test_lemmatize_text_no_tools(self, monkeypatch):
        # fallback test
        monkeypatch.setattr(tp, 'z_morph', None)
        monkeypatch.setattr(tp, 'z_zeyrek', None)
        text = "gelişmeleri takip etmekteyiz"
  
        assert tp.lemmatize_text(text) == text

    def test_lemmatize_text_with_mock_morphology(self, monkeypatch):
        # Test Zemberek Morphology
        mock_morph = MagicMock()
        
        # Mocking the analysis result 1
        mock_analysis1 = Mock()
        mock_analysis1.lemma = "gelişme"
        
        # Mocking the analysis result 2
        mock_analysis2 = Mock()
        mock_analysis2.lemma = "takip"
        
        # Mock the analyze method to return the specific mock results
        mock_morph.analyze.side_effect = [[mock_analysis1], [mock_analysis2]]
        
        monkeypatch.setattr(tp, 'z_morph', mock_morph)
        monkeypatch.setattr(tp, 'z_zeyrek', None) # Ensure z_zeyrek is not used
        
        text = "gelişmeleri takip"
        expected = "gelişme takip"
        
        result = tp.lemmatize_text(text)
        
        assert result == expected
        mock_morph.analyze.call_count == 2
        
    def test_lemmatize_text_with_mock_zeyrek(self, monkeypatch):
        # fallback morphology
        mock_zeyrek = Mock()
        # Mocking the lemmatized result
        mock_zeyrek.lemmatize.return_value = [("gelen", ["gel"]), ("bilgiye", ["bilgi"])]
        
        monkeypatch.setattr(tp, 'z_morph', None) # Ensure z_morph is not used
        monkeypatch.setattr(tp, 'z_zeyrek', mock_zeyrek)
        
        text = "gelen bilgiye"
        expected = "gel bilgi"
        
        result = tp.lemmatize_text(text)
        
        mock_zeyrek.lemmatize.assert_called_once_with(text)
        assert result == expected



class TestPipeline:

    def test_clean_tweet_full_pipeline(self, monkeypatch):
        # Setup mocks for the NLP parts 
        mock_normalizer = Mock()
        mock_normalizer.normalize.return_value = "normalized text"
        mock_morph = MagicMock()
        mock_morph.analyze.side_effect = lambda x: [Mock(lemma=x + "_lemma")]
        
        monkeypatch.setattr(tp, 'z_normalizer', mock_normalizer)
        monkeypatch.setattr(tp, 'z_morph', mock_morph)
        monkeypatch.setattr(tp, 'z_zeyrek', None) # Disable zeyrek
        
        # Test input
        raw_text = "Acil YARDIMM!!! Lütfeeeen geliiin, bakın: https://t.co/url #deprem"
        
        result = tp.clean_tweet(raw_text)
        
        mock_normalizer.normalize.assert_called_once() 
        
        assert result == "normalized_lemma text_lemma"


    def test_clean_tweet_fail_word_count(self, monkeypatch):
        # Ensure it returns an empty string if word count is too low
        raw_text = "#help acil" 
        monkeypatch.setattr(tp, 'z_normalizer', None)
        monkeypatch.setattr(tp, 'z_morph', None)

        result = tp.clean_tweet(raw_text, min_words=2) # 2 words > 2 is False
        assert result == ""
        
    def test_clean_tweet_fail_banned_keyword(self, monkeypatch):
        # Ensure it returns an empty string if a banned keyword is found
        raw_text = "lütfen allahım yardım etsin"
        monkeypatch.setattr(tp, 'z_normalizer', None)
        monkeypatch.setattr(tp, 'z_morph', None)
        
        # For example the key word "allahım" is banned
        result = tp.clean_tweet(raw_text)
        assert result == ""
