import com.ired.app.gtv.GtvWire;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/** Dependency-free wire fixtures; run with tools/test-gtv-wire.sh after assembling debug. */
public class GtvWireRegression {
    private static final GtvWire wire = GtvWire.INSTANCE;
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("Expected rejection");
    }
    public static void main(String[] args) {
        // Literal fixtures independently encode the v2 schema, including multi-byte tags.
        byte[] ack = {8, 2, 16, (byte)200, 1, 90, 0};
        wire.validatePairingResponse(ack, 11);
        byte[] secretAck = {8, 2, 16, (byte)200, 1, (byte)202, 2, 0};
        wire.validatePairingResponse(secretAck, 41);
        rejects(() -> wire.validatePairingResponse(ack, 41));
        rejects(() -> wire.validatePairingResponse(new byte[]{8,2,16,(byte)146,3}, 41));
        check(Arrays.equals(wire.buildOuterMessage(10, new byte[0], 2, 200),
            new byte[]{8,2,16,(byte)200,1,82,0}));
        check(Arrays.equals(wire.buildOptions(), new byte[]{10,4,8,3,16,6,24,1}));
        rejects(() -> wire.parseProtoFields(new byte[]{10,5,1}));
        rejects(() -> wire.parseProtoFields(new byte[]{(byte)128}));
        rejects(() -> wire.readDelimitedMessage(new ByteArrayInputStream(new byte[]{(byte)255,(byte)255,(byte)255,127})));
        check(wire.parseProtoFields(wire.buildRemoteKeyInject(23,3)).get(0).getNumber() == 10);
        System.out.println("Google TV wire regression checks passed (10 checks)");
    }
}
