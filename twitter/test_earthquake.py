import math
import pytest
import pandas as pd
import numpy as np

import new_kandilli_scrape as em 


@pytest.fixture
def quake_sample():
    return {
        'date': '2025.08.03',
        'time': '13:41:58',
        'latitude': '36.2197',
        'longitude': '36.2042',
        'depth_km': '10.0',
        'ML': '5.0',
        'Mw': '5.2'
    }


@pytest.fixture
def fake_cities(monkeypatch):
    # Simple cities dataframe
    data = pd.DataFrame({
        "name": ["Ankara", "Istanbul"],
        "latitude": [39.9334, 41.0082],
        "longitude": [32.8597, 28.9784],
        "feature_code": ["PPLC", "PPLA"],
        "country_code": ["TR", "TR"],
    })
    em.cities_data = data
    coords_rad = np.radians(data[["latitude", "longitude"]].values)
    em.ball_tree = em.BallTree(coords_rad, metric="haversine")
    return data

def test_get_last_earthquake_date(monkeypatch):
    class FakeTable:
        def get_item(self, Key):
            return {"Item": {"value": "2025.08.03"}}

    dt = em._get_last_earthquake_date(FakeTable())
    assert dt.year == 2025
    assert dt.month == 8
    assert dt.day == 3


def test_distance_to_nearest_settlement(fake_cities):
    dist, name = em._distance_to_nearest_settlement(39.9, 32.8)  # somewhere in Ankara
    assert name in ["Ankara", "Istanbul"]
    assert dist >= 0.0


def test_beta_and_a7(monkeypatch):
    # Mock boto3 S3 Object
    class FakeBody:
        def read(self):
            return b'{"c1":5,"a5":1.0,"a6":0.5,"a7":2.0}'

    class FakeObject:
        def get(self):
            return {"Body": FakeBody()}

    class FakeBoto3:
        def Object(self, bucket, key):
            return FakeObject()

    monkeypatch.setattr(em, "boto3", type("Boto3", (), {"resource": lambda *args, **kwargs: FakeBoto3()})())

    beta, a7 = em._beta_and_a7(6.0)
    assert isinstance(beta, float)
    assert isinstance(a7, float)
    assert math.isclose(a7, 2.0)


def test_determine_Svalue_and_ttl(fake_cities, monkeypatch, quake_sample):
    # Mock _beta_and_a7 and _is_offshore
    monkeypatch.setattr(em, "_beta_and_a7", lambda M: (1.1, 2.0))
    monkeypatch.setattr(em, "_is_offshore", lambda lat, lon: False)

    S, ttl = em._determine_earthquake_Svalue_and_ttl(quake_sample)
    assert S > 0
    assert ttl > 0


def test_calculate_danger_radius(monkeypatch, quake_sample):
    monkeypatch.setattr(em, "_beta_and_a7", lambda M: (1.1, 2.0))
    monkeypatch.setattr(em, "_is_offshore", lambda lat, lon: False)

    radius = em.calculate_earthquake_danger_radius(quake_sample)
    assert isinstance(radius, float)
    assert radius >= 0
