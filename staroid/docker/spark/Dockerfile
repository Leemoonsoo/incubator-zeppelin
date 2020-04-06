FROM moon/spark:2.4.5-SNAPSHOT

# remove -s option of tini. while gvisor does not support PR_SET_CHILD_SUBREAPER
RUN sed -i 's/tini -s/tini/' /opt/entrypoint.sh

ENTRYPOINT [ "/opt/entrypoint.sh" ]
