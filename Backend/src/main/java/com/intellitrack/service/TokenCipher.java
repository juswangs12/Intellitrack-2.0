package com.intellitrack.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class TokenCipher {
    private final String configuredKey;
    public TokenCipher(@Value("${google.token-encryption-key:}") String key){configuredKey=key;}
    public boolean configured(){try{return Base64.getDecoder().decode(configuredKey).length==32;}catch(Exception ex){return false;}}
    public String encrypt(String text){
        try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);var c=cipher(Cipher.ENCRYPT_MODE,iv);byte[] data=c.doFinal(text.getBytes(StandardCharsets.UTF_8));byte[] out=new byte[iv.length+data.length];System.arraycopy(iv,0,out,0,12);System.arraycopy(data,0,out,12,data.length);return Base64.getEncoder().encodeToString(out);}catch(Exception ex){throw new IllegalStateException("Google token encryption is not configured correctly");}
    }
    public String decrypt(String text){try{byte[] data=Base64.getDecoder().decode(text);return new String(cipher(Cipher.DECRYPT_MODE,Arrays.copyOf(data,12)).doFinal(Arrays.copyOfRange(data,12,data.length)),StandardCharsets.UTF_8);}catch(Exception ex){throw new IllegalStateException("Reconnect Google after restoring the token encryption key");}}
    private Cipher cipher(int mode,byte[] iv)throws Exception{
        if(!configured())throw new IllegalStateException();var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,new SecretKeySpec(Base64.getDecoder().decode(configuredKey),"AES"),new GCMParameterSpec(128,iv));return c;
    }
}
