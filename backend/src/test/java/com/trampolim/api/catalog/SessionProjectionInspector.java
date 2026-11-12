package com.trampolim.api.catalog;
/** SQL shape only; never captures parameter values or credentials. */
public final class SessionProjectionInspector implements org.hibernate.resource.jdbc.spi.StatementInspector {
 public static volatile String last="";
 @Override public String inspect(String sql){last=sql;return sql;}
}
