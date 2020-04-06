FROM moon/spark:2.4.5-SNAPSHOT

# remove -s option of tini. while gvisor does not support PR_SET_CHILD_SUBREAPER
RUN sed -i 's/tini -s/tini/' /opt/entrypoint.sh

ARG SPARK_USER_ID=2100
ARG SPARK_GROUP_ID=2100

RUN groupadd --gid $SPARK_GROUP_ID spark \
    && useradd -ms /bin/bash spark --uid $SPARK_USER_ID --gid $SPARK_GROUP_ID

RUN chown -R spark:spark /opt/spark/work-dir
USER 2100

ENTRYPOINT [ "/opt/entrypoint.sh" ]
