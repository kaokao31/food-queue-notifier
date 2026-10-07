import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class GenerateVapidKeys {

    public static void main(String[] args) throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));

        var pair = generator.generateKeyPair();
        var publicKey = (ECPublicKey) pair.getPublic();
        var privateKey = (ECPrivateKey) pair.getPrivate();

        byte[] publicBytes = new byte[65];
        publicBytes[0] = 4;

        System.arraycopy(
                to32Bytes(publicKey.getW().getAffineX()),
                0, publicBytes, 1, 32
        );
        System.arraycopy(
                to32Bytes(publicKey.getW().getAffineY()),
                0, publicBytes, 33, 32
        );

        var encoder = Base64.getUrlEncoder().withoutPadding();

        String content =
                "VAPID_PUBLIC_KEY=" + encoder.encodeToString(publicBytes)
                + System.lineSeparator()
                + "VAPID_PRIVATE_KEY="
                + encoder.encodeToString(to32Bytes(privateKey.getS()))
                + System.lineSeparator()
                + "VAPID_SUBJECT=mailto:replace-with-your-email@example.com"
                + System.lineSeparator();

        Files.writeString(
                Path.of(".env"),
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        System.out.println("Created .env. Keys were not printed.");
    }

    private static byte[] to32Bytes(BigInteger value) {
        byte[] source = value.toByteArray();
        byte[] result = new byte[32];
        int length = Math.min(source.length, 32);

        System.arraycopy(
                source, source.length - length,
                result, 32 - length, length
        );
        return result;
    }
}