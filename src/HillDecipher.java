import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.ModuloInteger;
import org.jscience.mathematics.vector.DenseMatrix;
import org.jscience.mathematics.vector.DenseVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;

public class HillDecipher {
    public static void main(String [] args){
        if (args.length < 5) throw new IllegalArgumentException("Arguments must be: <radix> <blocksize> <keyfile> <plainfile> <cipherfile>");

        try{
            int radix = Integer.parseInt(args[0]);
            int size = Integer.parseInt(args[1]);
            File plainTextFile = new File(args[3]);
            String keyString = readTxtFile(new File(args[2]));
            String requestedFileName = args[4];

            HillDecipherEngine decipherEngine = new HillDecipherEngine(radix,size, plainTextFile, keyString);
            int[] decryptedNumbers = decipherEngine.decrypt();
            for (int i: decryptedNumbers) System.out.print((char)(i+65)); //Debugging text

            writeToFile(decryptedNumbers, requestedFileName);
        }catch (NumberFormatException e){
            System.err.print("Args <radix> and <blocksize> must be integers");
        }
    }

    private static void writeToFile(int[] cryptoNumbers, String fileName) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
            BufferedWriter bufWriter = new BufferedWriter(writer);
            StringBuilder sb = new StringBuilder();
            for (int i : cryptoNumbers){ sb.append(i + " ");}
            String res = sb.toString();
            bufWriter.write(res.substring(0, res.length() - 1));
            bufWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readTxtFile(File plainText){
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(plainText));
            String st;
            while ((st = br.readLine()) != null) sb.append(st+"\n");
            System.out.print(sb.toString());
            while (sb.charAt(sb.length()-1) == 10) sb.deleteCharAt(sb.length()-1);
            return sb.toString();
        } catch (FileNotFoundException e) {
            System.err.println("The file '" + plainText.getName() + "' was not found. Quitting...\n");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return sb.toString();
    }
}
class HillDecipherEngine {
    int radix;
    int size;
    File plainTextFile;
    int readingProgress;
    int[][] keyMatrix;

    public HillDecipherEngine(int radix, int size, File plainTextFile, String keyString) {
        this.radix = radix;
        this.size = size;
        this.keyMatrix = parseKeyMatrix(size, keyString);
        this.plainTextFile = plainTextFile;
        readingProgress = -1;
    }

    private int[][] parseKeyMatrix(int size, String keyString) {
        String[] textLines = keyString.split("\\R");
        int[][] keyMatix = new int[size][size];
        if (textLines.length > size) throw new IllegalArgumentException("The key does not match <blocksize>");

        for (int line = 0; line < textLines.length; line++) {
            String[] numbers = textLines[line].split(" ");
            if (numbers.length > size) throw new IllegalArgumentException("The key does not match <blocksize>");
            if (numbers.length != textLines.length) throw new IllegalArgumentException("The key matrix is not square...");
            for (int x = 0; x < size; x++) {
                try {
                    keyMatix[line][x] = Integer.parseInt(numbers[x]);
                } catch (NumberFormatException e) {
                    System.err.print("One or more key matrix elements cannot be parsed as Integers..");
                    System.exit(1);
                }
            }
        }
        ModuloInteger.setModulus(LargeInteger.valueOf(radix));
        testKeyMatrixIsInvertible(keyMatix);
        return keyMatix;
    }

    private void testKeyMatrixIsInvertible(int[][] keyMatrix) {
        ModuloInteger.setModulus(LargeInteger.valueOf(radix));
        for (int[] row: keyMatrix) {
            for (int a : row) System.out.print(a + ", ");
            System.out.println();
        }
        ModuloInteger[][] values = new ModuloInteger[size][size];
        for(int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                values[i][j] = ModuloInteger.valueOf(
                        LargeInteger.valueOf(keyMatrix[i][j])
                );
            }
        }
        try {
            System.out.println();
            DenseMatrix<ModuloInteger> originalMatrix = DenseMatrix.valueOf(values);
            System.out.println("ORIGINAL INVERTED MATRIX");
            System.out.println(originalMatrix);
            originalMatrix.inverse();
        } catch (ArithmeticException e) {
            System.out.println(e);
            throw new ArithmeticException(e.getMessage());
        }
    }

    int[] decrypt() {
        LinkedList finalDecrypted = new LinkedList();
        try {
            RandomAccessFile fileReader = new RandomAccessFile(plainTextFile, "r");
            checkIfLastIndexIsNumber(fileReader);
            int tempByte;
            int offset = 0;
            int[] loadedNumbers = new int[size];
            StringBuilder currentRow = new StringBuilder();
            while (readingProgress < fileReader.length()) {
                fileReader.seek(++readingProgress);
                tempByte = fileReader.read();
                if (isSpaceOrNewLine(tempByte)) {
                    System.out.print(" ");
                    loadedNumbers[offset++] = Integer.parseInt(currentRow.toString());
                    currentRow.setLength(0);
                    if (offset == size) {
                        for (int i : decryptArray(loadedNumbers)) {
                            finalDecrypted.add(i);
                        };
                        offset = 0;
                    }
                } else {
                    System.out.print((char) tempByte);
                    currentRow.append((char) tempByte);
                }
            }
            return removePadding(finalDecrypted);
        } catch (FileNotFoundException e) {
            System.err.println("The file '" + this.plainTextFile.getName() + "' was not found. Shutting down...");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.print("One or more letter-numbers could not be parsed as Integers");
            System.exit(1);
        }
        return null;
    }

    private int[] removePadding(LinkedList finalDecrypted) {
        int paddingAdded = (int) finalDecrypted.get(finalDecrypted.size()-1);
        int finalLength = finalDecrypted.size()-paddingAdded;
        int[] result = new int[finalLength];
        for (int i = 0; i < finalLength; i++){
            result[i] = (int)finalDecrypted.get(i);
        }
        System.out.println("\ndecrypted"); // Debug text
        for (int j:result) System.out.print(j + ", ");
        return result;
    }

    private void checkIfLastIndexIsNumber(RandomAccessFile fileReader) {
        try {
            fileReader.seek(fileReader.length() - 1);
            char lastChar = ((char) (int) fileReader.read());
            if (lastChar < 48 || lastChar > 57) {
                System.err.print(lastChar + "Formatting of last character in input file is wrong. Check if 'space' or 'newline' has been added.");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isSpaceOrNewLine(int tempReadByte) {
        String s = String.valueOf((char) tempReadByte);
        return s.matches("\\s+") || tempReadByte == -1;
    }

    private int[] decryptArray(int[] loadedNumbers) {
        int[] sumOfRow = new int[size];
        for (int keyRow = 0; keyRow < keyMatrix.length; keyRow++) {
            for (int keyCol = 0; keyCol < keyMatrix[keyRow].length; keyCol++) {
                sumOfRow[keyRow] += (keyMatrix[keyRow][keyCol] * loadedNumbers[keyCol]);
            }
            sumOfRow[keyRow] = sumOfRow[keyRow] % radix;
        }
        return sumOfRow;
    }
}