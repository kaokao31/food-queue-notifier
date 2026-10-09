import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

/** Run with JDK21: java tools/GenerateVapidKeys.java NEW_OUTPUT_FILE */
public class GenerateVapidKeys {
  static byte[] fixed(java.math.BigInteger value){
    byte[] bytes=value.toByteArray(),out=new byte[32];
    System.arraycopy(bytes,Math.max(0,bytes.length-32),out,Math.max(0,32-bytes.length),Math.min(32,bytes.length));return out;
  }
  public static void main(String[] args) throws Exception {
    if(args.length!=1)throw new IllegalArgumentException("Specify a new output file, or --check for an in-memory key-pair check");
    var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));
    var pair=generator.generateKeyPair();var signature=Signature.getInstance("SHA256withECDSA");byte[] sample={1,2,3};
    signature.initSign(pair.getPrivate());signature.update(sample);byte[] signed=signature.sign();
    signature.initVerify(pair.getPublic());signature.update(sample);if(!signature.verify(signed))throw new IllegalStateException("Key pair check failed");
    if(args[0].equals("--check")){System.out.println("P-256 key pair signature check passed; no key material printed or file created");return;}
    var target=Path.of(args[0]);if(target.getFileName().toString().equalsIgnoreCase(".env"))throw new IllegalArgumentException("Choose a separate new file; .env is not an output target");
    byte[] publicKey=new byte[65];publicKey[0]=4;var point=((ECPublicKey)pair.getPublic()).getW();
    System.arraycopy(fixed(point.getAffineX()),0,publicKey,1,32);System.arraycopy(fixed(point.getAffineY()),0,publicKey,33,32);
    var encoder=Base64.getUrlEncoder().withoutPadding();
    String values="VAPID_PUBLIC_KEY="+encoder.encodeToString(publicKey)+"\nVAPID_PRIVATE_KEY="+
        encoder.encodeToString(fixed(((ECPrivateKey)pair.getPrivate()).getS()))+"\nVAPID_SUBJECT=mailto:REPLACE_WITH_YOUR_EMAIL\n";
    Files.writeString(target,values,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
    System.out.println("Created a new local key file. Keep it private; do not commit it. Set a valid VAPID_SUBJECT before use.");
  }
}
