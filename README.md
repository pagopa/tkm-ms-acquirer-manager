# Acquirer Manager
This microservice interacts with acquirers to handle their requests and add tokens into the TKM system.

## Configuration
The following ENVIRONMENT variables are needed to deploy and run the application.

- **DB_SERVER** *The database connection i.e. localhost:5000/tkm_acquirer_manager*
- **SERVER_PORT** *The spring boot port. Default value 8080*
- **ENABLE_KAFKA_APPENDER** *Uppercase boolean value that indicates if the logs is sent to the specific queue*
- **KAFKA_APPENDER_BOOTSTRAP_SERVERS** *The address of kafka broker connection i.e. localhost:9093*
- **KAFKA_APPENDER_TOPIC** *if ENABLE_KAFKA_APPENDER=TRUE is the queue topic name*
- **KAFKA_APPENDER_SECURITY_PROTOCOL** *Kafka security protocol. Default: SASL_SSL*
- **KAFKA_READ_QUEUE_TOPIC** *Topic name of read queue*
- **KAFKA_SECURITY_PROTOCOL** *Way to manage queue informations*
- **AZURE_KEYVAULT_PROFILE** *The prefix used to search for keys in the key vault (local/sit/uat/prod)*
- **AZURE_KEYVAULT_CLIENT_ID** *Azure Kevault authentication client id*
- **AZURE_KEYVAULT_CLIENT_KEY** *Azure Kevault authentication client key*
- **AZURE_KEYVAULT_TENANT_ID** *Azure Kevault authentication tenant id*
- **AZURE_KEYVAULT_URI** *Azure Kevault address*
- **CARD_MANAGER_URL** *Card manager url localhost:8080*
- **SFTP_SIA_HOSTNAME** *SIA SFTP hostname*
- **SFTP_SIA_PORT** *SIA SFTP port*
- **SFTP_SIA_USER** *SIA SFTP user*
- **SFTP_SIA_FOLDER** *SIA SFTP folder*
- **BLOB_STORAGE_BIN_HASH_CONTAINER** *Container where the BINs, HPANs and HTokens are held*
- **BLOB_STORAGE_ACQUIRER_CONFIG_CONTAINER** *Container where the acquirer configurations are held*
- **BIN_RANGE_RETRIEVAL_CRON** *Cron for the BIN range retrieval batch*
- **BIN_RANGE_GEN_CRON** *Cron for the BIN range file generation batch*
- **HTOKEN_HPAN_COPY_CRON** *Cron for the known hashes files relocation batch*
- **BATCH_ACQUIRER_RESULT_CRON** *Cron for the acquirer file handling batch*
- **KNOWN_HASHES_GEN_CRON** *Cron for the known hashes retrieval (from Card Manager) batch*
- **BATCH_MAX_THREAD** *Max number of threads to be allocated to this microservice's batches*

### Develop enviroment configuration
- Set **-Dspring.profiles.active=local** as jvm setting
- Add as enviroment variable **AZURE_KEYVAULT_CLIENT_ID=~~VALUE_TO_ADD~~;AZURE_KEYVAULT_CLIENT_KEY=~~VALUE_TO_ADD~~;AZURE_KEYVAULT_TENANT_ID=~~VALUE_TO_ADD~~;AZURE_KEYVAULT_URI=~~VALUE_TO_ADD~~**

## How to start SIT azure pipeline

1. Move into:
> develop

1. Run:<br>
   `$version=??` for poweshell or `version=??` for gitbash<br>
   `mvn --batch-mode release:clean release:prepare`<br>
   `git checkout -b tmp/${version} acquirer-manager-${version}`<br>
   `git push --set-upstream origin tmp/${version}`<br>

2. Merge **tmp/${version}** into **release/sit**

## How to start UAT azure pipeline

1. Merge **release/sit** into **release/uat**

## How to start PROD azure pipeline

1. Merge **release/uat** into **master**