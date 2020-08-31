FROM azul/zulu-openjdk:11

ARG BUNDLE_ZIP
COPY ${BUNDLE_ZIP} /tmp/m2m2-core.zip
ENV MA_HOME=/opt/mango
RUN mkdir -p "$MA_HOME" \
    && cd "$MA_HOME" \
    && jar xf /tmp/m2m2-core.zip \
    && rm /tmp/m2m2-core.zip

EXPOSE 8080
WORKDIR "$MA_HOME"
ENV CLASSPATH="$MA_HOME/overrides/classes:$MA_HOME/classes:$MA_HOME/overrides/properties:$MA_HOME/overrides/lib/*:$MA_HOME/lib/*"
ENTRYPOINT java -server -Xmx1g com.serotonin.m2m2.Main