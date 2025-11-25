#!/bin/bash
# Simple script to run a real crawl using SearXNG

cd /Users/kevin/github/northstar-funding

# Run the crawler with SearXNG adapter directly
mvn exec:java -pl northstar-crawler \
  -Dexec.mainClass="com.northstar.funding.crawler.SimpleCrawlRunner" \
  -Dexec.args="EU funding opportunities Bulgaria 2025"
