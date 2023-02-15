# Descrizione
Il microservizio (Acquirer Manager) permette agli acquirer di recuperare le informazioni necessarie tramite API RESTful. Inoltre il servizio si occupa di recuperare ed elaborare il file prodotto dagli issuer.

# Configurazioni
Le seguenti variabile d'ambiente permettono di configurare il servizio e adattarlo alle eseginze del momento

- **AZURE_KEYVAULT_PROFILE**: rappresenta il profilo da utilizzare per il recupero dei valori contenuti nel key vault. Sono ammessi i valori 'local', 'sit', 'uat', 'prod'
- **KAFKA_APPENDER_TOPIC**: Non del topic sulla quale scrivere i log.
- **AZURE_STORAGE_ENDPOINT**: Url completo del blob storage
- **VISA_URL_ACQUIRER**: URL di VISA per il recuper dei bin range.
- **DB_SERVER**: Stringa di connessione al database
- **LOGGING_LEVEL**: livello root del log
- **KAFKA_READ_QUEUE_TOPIC**: Nome del topic nel quale sono scritti i messaggi con destinazione il card-manager
- **KAFKA_SERVERS**: IP/HOSTNAME del server delle code
- **CARD_MANAGER_URL**: URL completo del card manager
- **BLOB_STORAGE_BIN_HASH_CONTAINER**: Nome del container all'interno del blob storage che mantiene i file dei bin e gli hash
- **BLOB_STORAGE_ACQUIRER_CONFIG_CONTAINER**: Nome del container aventi le configurazioni degli acquirer
- **BIN_RANGE_RETRIEVAL_CRON**: Stringa di crontab per il recupero dei bin range
- **BIN_RANGE_GEN_CRON**: Stringa di crontab per la generazione del file contenente i bin range
- **HTOKEN_HPAN_COPY_CRON**: Stringa di crontab per la messa a disposizione del file contenete gli hash e con destinazione gli acquirer
- **BATCH_ACQUIRER_RESULT_CRON**: Stringa di crontab per l'elaborazione dei file inviata dagli acquirer
- **KNOWN_HASHES_GEN_CRON**: Stringa di crontab per la generazione dei file contenete gli hash
- **BATCH_MAX_THREAD**: Numero di thread destinati all'esecuzione dei batch in parallelo
- **AZURE_KEYVAULT_URI**: URL del keyvault azure
- **AZURE_KEYVAULT_CLIENT_ID**: client id  del keyvault azure
- **AZURE_KEYVAULT_CLIENT_KEY**: client key  del keyvault azure
- **AZURE_KEYVAULT_TENANT_ID**: tenant id  del keyvault azure


## How to start SIT azure pipeline

1. Merge **feature branch** into **develop**<br>
   Pipeline starts automatically and do maven prepare release.<br>
   At the end, the pipeline create branch tmp/<version><br>

   If you have to do manually, run:<br>
   `$version=??` for poweshell or `version=??` for gitbash<br>
   `mvn --batch-mode release:clean release:prepare -DscmCommentPrefix="[skip ci]"`<br>
   `git push origin develop`<br>
   `git push origin --tags`<br>
   `git checkout -b tmp/${version} acquirer-manager-${version}`<br>
   `git push --set-upstream origin tmp/${version}`<br>

2. Merge **tmp/${version}** into **release/sit**

## How to start UAT azure pipeline

1. Merge **release/sit** into **release/uat**

## How to start PROD azure pipeline

1. Merge **release/uat** into **master**



## How to make a fix

1. Create a new branch **PM-XXXX-XXXXX** from master/production tag that need to be fixed
2. Prepare fix and push it onto branch **PM-XXXX-XXXXX**
   3. SIT RELEASE
      1. Merge branch **PM-XXXX-XXXXX** into **develop**
         Pipeline starts automatically and do maven prepare release.
         At the end, the pipeline create branch **tmp/<version>**
      2. Merge **tmp/${version}** into **release/sit**
   4. UAT RELEASE
      1. Update "X.XX.XX" POM version to "X.XX.XX-fix-vXX" into branch **PM-XXXX-XXXXX**
      2. Create a new branch **hotfix/X.XX.XX-fix-vXX** from branch **PM-XXXX-XXXXX**
      3. PROD RELEASE 
         1. Merge **hotfix/X.XX.XX-fix-vXX** into **master**


## How to do a rollback

UAT
   1. Create a new branch **rollback/uat/X.YY.ZZ** from tmp version **tmp/X.YY.ZZ** that need to be restored into UAT
   ATTENTION:
      1. DB updates are not rollbacked to version **tmp/X.YY.ZZ**
      2. Branch **release/uat** is now out of date and will need to be realigned

PROD
1. Create a new branch **rollback/prod/X.YY.ZZ** from tmp version **tmp/X.YY.ZZ** that need to be restored into PROD
   ATTENTION:
   1. DB updates are not rollbacked to version **tmp/X.YY.ZZ**
   2. Branch **master** is now out of date and will need to be realigned