docker run -d \
  --name pg17-vector \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=123456 \
  -v /data/pg17-vector:/var/lib/postgresql/data \
  pgvector/pgvector:pg17

docker run -d --name emqx -p 1883:1883 -p 18083:18083 emqx:latest

mvn -version
mvn package -DskipTests
java -version
java -jar target\smart-street-light-0.0.1-SNAPSHOT.jar