# This script is used to scrape the tweets for the earthquake on Feb 6 2023.
# It is used to generate the example data for the table.
# NOTE: This script DOES NOT WORK with Python version 3.12 or greater due to snscrape's outdated system and lack of support for newer Python versions.

import os
from dotenv import load_dotenv
import boto3
import json
import subprocess

load_dotenv()

# DynamoDB
dynamodb = boto3.resource("dynamodb")
mock_data_table = dynamodb.Table(os.getenv("MOCK_DATA_TABLE_NAME"))

# Query parameters
keywords = "#deprem (#yardım OR ihtiyac OR ihtiyaç OR enkaz OR erzak)"
date = "since:2023-02-06 until:2023-02-13"
filtering = "lang:tr -is:retweet"

# Command to run
cmd = [
    "snscrape",
    "--jsonl",
    "--max-results", "500",
    "twitter-search",
    keywords + " " + date + " " + filtering
]

# Run the command and get the result
result = subprocess.run(cmd, capture_output=True, text=True)

print("Running command: " + " ".join(cmd))
print("result.stdout: " + result.stdout)
print("result.stderr: " + result.stderr)

# Read, parse, and write the tweets to the table
written_count = 0
for line in result.stdout.strip().split("\n"):
    try:
        tweet = json.loads(line)
        tweet_id = str(tweet["id"])
        text = tweet["rawContent"]
        created_at = tweet["date"]
        user = tweet["user"]["username"]
        hashtags = tweet.get("hashtags", [])

        mock_data_table.put_item(Item={
            "tweet_id": tweet_id,
            "text": text,
            "created_at": str(created_at),
            "user": user,
            "hashtags": hashtags
        })

        written_count += 1

        if written_count % 50 == 0:
            print(f"Added {written_count} tweets to the table")
    except Exception as e:
        print(f"Error processing tweet {tweet_id if tweet_id else 'unknown'}: {e}\nError occured in line: {line}")
        continue

print(f"Added {written_count} tweets to the table")