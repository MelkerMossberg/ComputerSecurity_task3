import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.ModuloInteger;
import org.jscience.mathematics.vector.DenseMatrix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class HillKeys {
    private static int radix = -1;
    private static int size = -1;
    private static String requestedFileName = "";
    public static void main(String[] args){
        if(args.length != 3){
            throw new IllegalArgumentException("Arguments must be: <radix> <blocksize> <keyfile>");
        } else {
            parseArgs(args);
        }
        verifyArguments();

        ModuloInteger.setModulus(LargeInteger.valueOf(radix));
        DenseMatrix<ModuloInteger> generatedKey = null;
        while(generatedKey == null || !isInvertible(generatedKey)){
            generatedKey = generateRandomMatrix();
        }
        writeKeyToFile(generatedKey);
    }

    private static void writeKeyToFile(DenseMatrix<ModuloInteger> key) {
        File output = new File(requestedFileName);
        FileWriter encryptedWriter;
        try {
            encryptedWriter = new FileWriter(output);
            for(int a = 0; a < key.getNumberOfColumns(); a++){
                for(int b = 0; b < key.getNumberOfRows(); b++) {
                    if(b != 0){
                        encryptedWriter.write(" ");
                        System.out.print(" ");
                    }
                    encryptedWriter.write(key.get(a, b).toString());
                    System.out.print(key.get(a, b).toString());
                }
                if(a != key.getNumberOfColumns() - 1) {
                    encryptedWriter.write("\n");
                    System.out.println();
                }
            }
            System.out.println("\n");
            encryptedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static DenseMatrix<ModuloInteger> generateRandomMatrix() {
        ModuloInteger[][] values = new ModuloInteger[size][size];

        for(int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                values[i][j] = ModuloInteger.valueOf(
                        LargeInteger.valueOf(ThreadLocalRandom.current().nextInt(0, radix))
                );
            }
        }

        return DenseMatrix.valueOf(values);
    }

    private static void verifyArguments() {
        if(!(radix > 0 && radix <= 256) || !(size > 0 && size <= 8)) {
            throw new IllegalArgumentException("Unexpected arguments! Radix must be between 1 and 256, block size must be between 1 and 8!");
        }
    }

    private static void parseArgs(String[] args) {
        radix = Integer.parseInt(args[0]);
        size = Integer.parseInt(args[1]);
        requestedFileName = args[2];
    }

    private static boolean isInvertible(DenseMatrix<ModuloInteger> matrix) {
        try {
            //System.out.println(matrix.inverse().toString().replaceAll("[{]|[}]", ""));
            matrix.inverse();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }
}
