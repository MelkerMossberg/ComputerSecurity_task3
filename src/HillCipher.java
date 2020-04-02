import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

public class HillCipher {
    public static void main(String [] args){
        if (args.length < 5) throw new IllegalArgumentException("Arguments must be: <radix> <blocksize> <keyfile> <plainfile> <cipherfile>");
        try{
            int radix = Integer.parseInt(args[0]);
            int size = Integer.parseInt(args[1]);
            System.out.println("Size: " + size); //Debug
            File plainTextFile = new File(args[3]);
            String keyString = readTxtFile(new File(args[2]));
            String requestedFileName = args[4];

            HillCipherEngine cipherEngine = new HillCipherEngine(radix,size, plainTextFile, keyString);
            int[] cryptoNumbers = cipherEngine.encrypt();
            for (int i: cryptoNumbers) System.out.print((char)(i+65));

            writeToFile(cryptoNumbers, requestedFileName);
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
class HillCipherEngine {
    int radix;
    int blocksize;
    File plainTextFile;
    int readingProgress;
    int[][] keyMatrix;
    int howLongToRead;

    public HillCipherEngine(int radix, int size, File plainTextFile, String keyString) {
        this.radix = radix;
        this.blocksize = size;
        this.keyMatrix = parseKeyMatrix(size, keyString);
        this.plainTextFile = plainTextFile;
        readingProgress = -1;
    }

    private int[][] parseKeyMatrix(int size, String keyString) {
        String[] textLines = keyString.split("\\R");
        int[][] keyMatix = new int[size][size];
        if (textLines.length > size) {
            System.out.println("Num of rows in key does not match size");
            return null;
        }
        for (int line = 0; line < textLines.length; line++) {
            String[] numbers = textLines[line].split(" ");
            if (numbers.length > size) {
                System.out.println("Num of columns in key do not match size");
                return null;
            }
            for (int x = 0; x < size; x++) {
                try {
                    keyMatix[line][x] = Integer.parseInt(numbers[x]);
                } catch (NumberFormatException e) {
                    System.err.print("One or more key matrix elements cannot be parsed as Integers..");
                    System.exit(1);
                }

            }
        }
        return keyMatix;
    }

    public int[] encrypt() {
        LinkedList finalCrypto = new LinkedList();
        try {
            RandomAccessFile fileReader = new RandomAccessFile(plainTextFile, "r");
            validateFormatFirstAndLastIndex(fileReader);
            int tempByte;
            int offset = 0;
            int[] loadedNumbers = new int[blocksize];
            StringBuilder currentFullNumber = new StringBuilder();
            int numOfNumbersAdded = 0;
            while (readingProgress < howLongToRead) {
                fileReader.seek(++readingProgress);
                tempByte = fileReader.read();
                if (isSpaceOrNewLine(tempByte)) {
                    System.out.print(" "); // Debug print
                    numOfNumbersAdded++;
                    int integerToAdd = Integer.parseInt(currentFullNumber.toString());
                    loadedNumbers[offset++] = integerToAdd;
                    currentFullNumber.setLength(0);
                    if (offset == blocksize) {
                        for (int i : encryptArray(loadedNumbers)) {
                            finalCrypto.add(i);
                        }
                        offset = 0;
                        Arrays.fill(loadedNumbers, -1);
                    }
                } else {
                    System.out.print((char) tempByte); // Debug print
                    currentFullNumber.append((char) tempByte);
                }
            }
            return addPadding(finalCrypto,loadedNumbers, numOfNumbersAdded);

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
        System.exit(1);
        return null;
    }

    private int[] addPadding(LinkedList finalCryptoList, int[] currentlyLoaded, int totalNumRead) {
        int restSize = totalNumRead % blocksize;
        int numbersToPad = blocksize - restSize;
        for (int i = 0; i < numbersToPad-1; i++){
            currentlyLoaded[restSize + i] = addRandomNum();
        }
        currentlyLoaded[currentlyLoaded.length-1] = numbersToPad;
        for (int i : encryptArray(currentlyLoaded)) {
            finalCryptoList.add(i);
        }
        int[] result = new int[finalCryptoList.size()];
        for (int y = 0; y < finalCryptoList.size(); y ++) result[y] = (int) finalCryptoList.get(y);

        return result;
    }

    private int addRandomNum() {
        return new Random().nextInt(radix+1);
    }

    private void validateFormatFirstAndLastIndex(RandomAccessFile fileReader) {
        try {
            this.howLongToRead = ((int)fileReader.length());
            fileReader.seek(0);
            char firstChar = ((char) (int) fileReader.read());
            if (firstChar < 48 || firstChar > 57) {
                System.err.print(firstChar + "Formatting of first character in input file is wrong.");
                System.exit(1);
            }
            fileReader.seek(fileReader.length() - 1);
            char lastChar = ((char) (int) fileReader.read());
            if (lastChar < 48) {
                System.out.println(lastChar + "The last character was 'newline', thus removed. Continue running.");
                this.howLongToRead = howLongToRead -1;
            }
            if (lastChar > 57) {
                System.err.print(lastChar + "Formatting of last character in input file is wrong. Check if 'space' has been added.");
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

    private int[] encryptArray(int[] loadedNumbers) {
        int[] sumOfRow = new int[blocksize];
        for (int keyRow = 0; keyRow < keyMatrix.length; keyRow++) {
            for (int keyCol = 0; keyCol < keyMatrix[keyRow].length; keyCol++) {
                sumOfRow[keyRow] += (keyMatrix[keyRow][keyCol] * loadedNumbers[keyCol]);
            }
            sumOfRow[keyRow] = sumOfRow[keyRow] % radix;
        }
        return sumOfRow;
    }
}

