echo 'Waiting for DB to be operational...'
sleep 100s

./liquibase/liquibase --changeLogFile=./changelogs/acquirer-manager/master-changelog.xml --url="jdbc:postgresql://host.docker.internal:5000/tkm_acquirer_manager" --username=tkm_acquirer_manager --password=tkm_acquirer_manager --contexts="tag,baseline,insert,incremental,insert-dev,incremental-dev" --log-level=INFO --driver=org.postgresql.Driver --classpath=./liquibase/postgresql-42.5.0.jar update