package com.trampolim.api.trust;
import java.util.Locale;
/** Syntax/check digits only. Never proves registration, ownership or trustworthiness. */
public final class Cnpj {
 private Cnpj(){}
 public static String normalize(String value){if(value==null)return "";String raw=value.replaceAll("[./ \t-]","");return raw.matches("[A-Za-z0-9]*")?raw.toUpperCase(Locale.ROOT):raw;}
 public static boolean valid(String value){String n=normalize(value);if(!n.matches("[A-Z0-9]{12}[0-9]{2}")||n.chars().distinct().count()==1)return false;return digit(n.substring(0,12))==n.charAt(12)-'0'&&digit(n.substring(0,13))==n.charAt(13)-'0';}
 private static int digit(String n){int weight=2,sum=0;for(int i=n.length()-1;i>=0;i--){sum+=(n.charAt(i)-48)*weight;if(++weight>9)weight=2;}int remainder=sum%11;return remainder<2?0:11-remainder;}
}
