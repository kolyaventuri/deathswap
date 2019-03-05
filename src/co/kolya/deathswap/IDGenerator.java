package co.kolya.deathswap;

import java.security.SecureRandom;

public class IDGenerator {
	static final String pool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static SecureRandom rnd = new SecureRandom();

	static String random(int len){
	   StringBuilder sb = new StringBuilder(len);
	   int length = pool.length();
	   
	   for(int i = 0; i < len; i++) {
		  int index = rnd.nextInt(length);
		  char nextChar = pool.charAt(index);
		  
		  sb.append(nextChar);
	   }
	   
	   return sb.toString();
	}
}
