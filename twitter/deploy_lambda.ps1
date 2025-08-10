# PowerShell script to deploy lambda functions to AWS
# Run .\deploy_lambda.ps1 to run the script and deploy the lambda function to AWS

$lambda_function_name = "tweet_scraper_for_seismiq"
$main_script = "twitter_scraper.py"
$deploy_dir = "lambda_deps"
$zip_file = "lambda_deps\lambda_deploy.zip"

try {
    pip install -r requirements.txt -t $deploy_dir
    Write-Host "Installed dependenies"

    # Copy the main script to the deploy directory
    Copy-Item -Path $main_script -Destination $deploy_dir -Force

    # Check if the zip file already exists, if so remove it
    if (Test-Path $zip_file) {Remove-Item $zip_file}

    # Zip the contents of deploy directory
    Compress-Archive -Path "$deploy_dir\*" -DestinationPath $zip_file
    Write-Host "Zipped the contents of deploy directory"

    # Upload the Lambda function
    aws lambda update-function-code --function-name $lambda_function_name --zip-file fileb://$zip_file
    Write-Host "Uploaded the Lambda function"
}
catch {
    Write-Host "Deployment failed"
    exit 1
}

