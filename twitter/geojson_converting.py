import geopandas as gpd

gdf = gpd.read_file("twitter/ne_10m_land/ne_10m_land.shp") # This directory is deleted because I don't want to mess up the repo.
gdf = gdf.to_crs(epsg=4326)
gdf.to_file("twitter/ne_10m_land.geojson", driver="GeoJSON") # This is also deleted from the codebase, it is stored in S3

print("GeoJSON file created successfully.")