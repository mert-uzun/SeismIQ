import pandas as pd

COLUMNS = [
    "geonameid","name","asciiname","alternatenames","latitude","longitude",
    "feature_class","feature_code","country_code","cc2","admin1_code",
    "admin2_code","admin3_code","admin4_code","population","elevation",
    "dem","timezone","modification_date"
]

REGIONS = {"TR"} # Only Turkey for now
 
KEEP_FC = {"P"} # Populated place

def _process_geoname_cities5000(input_file_path: str, output_file_path: str):
    df = pd.read_csv(input_file_path, 
                    sep="\t", names=COLUMNS, header=None, dtype={
                    "latitude":"float64","longitude":"float64",
                    "feature_class":"string","feature_code":"string",
                    "country_code":"string","name":"string","asciiname":"string",
                    "population":"Int64"}, 
                    encoding="utf-8",
                    low_memory=False)
 
    df = df[df["country_code"].isin(REGIONS)]
    df = df[df["feature_code"].isin(KEEP_FC)]

    df = df.dropna(subset=["latitude", "longitude", "population", "name"])

    df = df[["geonameid", "name", "latitude", "longitude", "country_code", "feature_code", "population"]]

    df.to_parquet(output_file_path, index=False)

    print(f"Processed {len(df)} rows and saved to {output_file_path} path destination.")
    
if __name__ == "__main__":
    _process_geoname_cities5000("twitter/cities5000.txt", "twitter/cities5000.tr-region.v1.parquet")