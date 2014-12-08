
insert into dbm_log (id, tstamp, filepath, success, num_rows, sql, error)
  select id, tstamp, filepath, status, num_rows, sql, error from dbm_log_v1;
  
insert into dbm_versions(id, tstamp, filepath, action, hostname, hostuser, version)
  select id, tstamp, filepath, action, hostname, username, version from dbm_versions_v1;
  
