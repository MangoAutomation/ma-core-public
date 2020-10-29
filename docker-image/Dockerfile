FROM azul/zulu-openjdk:11

ENV MA_HOME=/opt/mango
WORKDIR "$MA_HOME"
EXPOSE 8080
ENV CLASSPATH="$MA_HOME/overrides/classes:$MA_HOME/classes:$MA_HOME/overrides/properties:$MA_HOME/overrides/lib/*:$MA_HOME/lib/*"
ENTRYPOINT ["java"]
CMD ["com.serotonin.m2m2.Main"]

ARG BUNDLE_ZIP
COPY ${BUNDLE_ZIP} mango-bundle.zip
RUN jar xf mango-bundle.zip && \
rm mango-bundle.zip && \
cd web/modules && \
for f in *.zip; do tmp=${f#m2m2-}; name=${tmp%%-*}; mkdir $name; cd $name; jar xf ../$f; cd ..; rm $f; done
