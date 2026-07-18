package net.opmasterleo.license.examples;

import net.opmasterleo.license.internal.crypto.Concealed;

public final class ConcealSecrets {

    private ConcealSecrets() {
    }

    public static void main(String[] args) {
        int seed = (int) (System.nanoTime() & 0x7FFFFFFF);
        if (seed == 0) {
            seed = 0x5F3759DF;
        }

        System.out.println("seed = " + seed);
        for (String value : args) {
            int[] encoded = Concealed.encode(value, seed);
            System.out.println("\"" + value + "\" ->");
            System.out.print("new int[] { ");
            for (int i = 0; i < encoded.length; i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(encoded[i]);
            }
            System.out.println(" }");
        }
        System.out.println("\nUsage: Concealed.decode(encoded, " + seed + ")");
    }
}
