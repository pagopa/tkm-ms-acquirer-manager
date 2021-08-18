# Descrizione
Il microservizio (Acquirer Manager) permette agli acquirer di recuperare le informazioni necessarie tramite API RESTful. Inoltre il servizio si occupa di recuperare ed elaborare il file prodotto dagli issuer.

# Configurazioni
Le seguenti variabile d'ambiente permettono di configurare il servizio e adattarlo alle eseginze del momento

- **AZURE_KEYVAULT_PROFILE**: rappresenta il profilo da utilizzare per il recupero dei valori contenuti nel key vault. Sono ammessi i valori 'local', 'sit', 'uat', 'prod'
- **KAFKA_APPENDER_TOPIC**: Non del topic sulla quale scrivere i log.
- **AZURE_STORAGE_ENDPOINT**: Url completo del blob storage
- **VISA_URL**: URL di VISA per il recuper dei bin range.
- **DB_SERVER**: Stringa di connessione al database
- **LOGGING_LEVEL**: livello root del log
- **SFTP_SIA_HOSTNAME**: IP/hostname del server SFTP nel quale sono depositati i file degli acquirer
- **SFTP_SIA_PORT**: Porta del server SFTP nel quale sono depositati i file degli acquirer
- **SFTP_SIA_USER**: Username del server SFTP nel quale sono depositati i file degli acquirer
- **SFTP_SIA_FOLDER**: Cartella del server SFTP nel quale sono depositati i file degli acquirer
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

## Avvio della pipeline azure in ambiente di SIT

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