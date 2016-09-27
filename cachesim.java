import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class cacheSim {

	// vars for all the global variables
	private static int cacheSize;  //size of cache
	private static int associativity; //# of ways
	private static int blockSize;  // size of blocks
	private static String[] ram = new String[16777215]; //initialize ram as String[]
	private static aBlock[][] cache; //cache is an array of an array of blocks 
	private static int currentAdd; //current Address in decimal
	private static int currentBlock; //current block starting point that address lives in 
	private static String newData; //the new data
	private static int byteLength; //how long the data extends
	private static int currentSet; //current Set in cache for incoming instruction address
	private static int currentTag; //current Tag for incoming instruction
	private static String hexAdd; //address in hex

	//class for a set with valid bit and tag
	public static class aBlock {
		private int valid;
		private int tag;
		private String[] data;


		public aBlock() {
			valid = 0;
		}

	}

	//initialize the cache using empty sets
	public static void makeCache() {
		int numberOfSets = (1024 * cacheSize)/blockSize/associativity;
		cache = new aBlock[numberOfSets][];
		for (int i = 0; i < numberOfSets; i++) {
			cache[i] = new aBlock[associativity];
			for (int k = 0; k < associativity; k++) {
				cache[i][k] = new aBlock();
			}
		}
	}

	//initialize all data in RAM to 00
	public static void initializeRAM() {
		for (int i = 0; i < ram.length; i++) {
			ram[i] = "00";
		}
		return;
	}

	//exponential function
	public static int power(int a, int b) {
		int finalValue = a;
		int current;
		if (b == 0) {
			return 1;
		}
		for (int i = 1; i < b; i++) {
			finalValue = finalValue * a;
		}
		return finalValue;
	}

	//translates address to correct index in decimal
	public static int addressTranslation(String address) {
		String add = address.toUpperCase();
		int exponent = address.length() -1;
		String alpha = "0123456789ABCDEF";
		int finalValue = 0;
		int current;
		for (int i = 0; i < address.length(); i++) {
			current = alpha.indexOf(add.charAt(i));
			finalValue += (current * Math.pow(16, exponent));
			exponent-=1;
		}
		return finalValue;
	}

	//trnaslates address to get the starting point of the block it lives in
	public static int blockTranslation(int address) {
		int current = address;
		int blockPoint = 0;
		while (current > blockSize -1) {
			current -= blockSize;
			blockPoint += blockSize;
		}
		return blockPoint;
	}

	//translates the address to find the set it lives in 
	public static int setTranslation(int address) {
		int numberOfSets = (1024 * cacheSize)/blockSize/associativity;
		int thePoint = address;
		thePoint = thePoint/blockSize;
		currentTag = thePoint/numberOfSets;
		thePoint = thePoint%numberOfSets;
		return thePoint;
	}

	//retrieves the hex data from a block
	public static String getData(String[] data, int byteOffset) {
		String output = "";
		int current = byteOffset;
		for (int i = 0; i < byteLength; i++) {
			output += data[current];
			current += 1;
		}
		return output;
	}

	//least recently used algorithm. Way it works: if recentAccess == -1,
	//knows that it is a new block that needs to be inserted, so moves everything
	//back and places it in front. most recently used block in front of array.
	//if not -1, knows that it is already in the array, so moves every block back then 
	//inserts it in front. 
	public static aBlock[] lru(aBlock[] theArray, int recentAccess) {
		aBlock[] current = theArray;
		if (recentAccess == 0) {
			return current;
		} else if (recentAccess == -1) {
			aBlock newBlock = new aBlock();
			newBlock.valid = 1;
			newBlock.data = Arrays.copyOfRange(ram, currentBlock, currentBlock+blockSize);
			newBlock.tag = currentTag;
			for (int i = current.length-1; i > -1; i--) {
				if (i == 0) {
					current[i] = newBlock;
				} else {
					current[i] = current[i-1];
				}
			}
		} else {
			aBlock updateBlock = current[recentAccess];
			for (int i = recentAccess; i > -1; i--) {
				if (i == 0) {
					current[i] = updateBlock;
				} else {
					current[i] = current[i-1];
				}
			} 
		}
		return current;
	}

	//changes data in an actual block
	public static String[] changeData(String[] oldData) {
		String updateData = newData;
		String[] updatedString = oldData;
		int byteOffset = currentAdd-currentBlock;
		for (int i = byteOffset; i < byteOffset+ byteLength; i++) {
			updatedString[i] = updateData.substring(0, 2);
			updateData = updateData.substring(2);
		}
		return updatedString;
	}

	//changes data in main memory
	public static void changeRAM() {
		String updateData = newData;
		for (int i = currentAdd; i < currentAdd + byteLength; i++) {
			ram[i] = updateData.substring(0, 2);
			updateData = updateData.substring(2);
		}
		return;
	}


	//store method
	public static void store() {
		aBlock[] cacheSet = cache[currentSet];
		//System.out.println(currentSet);
		aBlock current;
		String output = "";
		for (int i = 0; i < cacheSet.length; i++) {
			current = cacheSet[i];
			if (current.valid == 1) {
				if (current.tag == currentTag) {
					output += "store " + hexAdd + " hit";
					System.out.println(output);
					cache[currentSet][i].data = changeData(current.data);
					changeRAM();
					//System.out.println(Arrays.deepToString(cacheSet));
					cache[currentSet] = lru(cacheSet, i);
					//System.out.println(Arrays.deepToString(cacheSet));
					return;
				}
			} else {
				output += "store " + hexAdd + " miss";
				System.out.println(output);
				changeRAM();
				//System.out.println(Arrays.deepToString(cacheSet));
				//cache[currentSet] = lru(cacheSet, -1);
				//System.out.println(Arrays.deepToString(cacheSet));
				return;
			}
		}
		output += "store " + hexAdd + " miss";
		System.out.println(output);
		changeRAM();
		//System.out.println(Arrays.deepToString(cacheSet));
		//cache[currentSet] = lru(cacheSet, -1);
		//System.out.println(Arrays.deepToString(cacheSet));
		return;
	}


	//load method
	public static void load() {
		aBlock[] cacheSet = cache[currentSet];
		aBlock current;
		String output = "";
		for (int i = 0; i < cacheSet.length; i++) {
			current = cacheSet[i];
			if (current.valid == 1) {
				if (current.tag == currentTag) {
					String data = getData(current.data, currentAdd-currentBlock);
					output += "load " + hexAdd + " hit " + data;
					System.out.println(output);
					//System.out.println(Arrays.deepToString(cacheSet));
					cache[currentSet] = lru(cacheSet, i);
					//System.out.println(Arrays.deepToString(cacheSet));
					return;
				}
			} else {
				String data = getData(Arrays.copyOfRange(ram, currentBlock, currentBlock+blockSize),currentAdd-currentBlock);
				output += "load " + hexAdd + " miss " + data;
				System.out.println(output);
				//System.out.println(Arrays.deepToString(cacheSet));
				cache[currentSet] = lru(cacheSet, -1);
				//System.out.println(Arrays.deepToString(cacheSet));
				return;
			}
		}
		String data = getData(Arrays.copyOfRange(ram, currentBlock, currentBlock+blockSize),currentAdd-currentBlock);
		output += "load " + hexAdd + " miss " + data;
		System.out.println(output);
		//System.out.println(Arrays.deepToString(cacheSet));
		cache[currentSet] = lru(cacheSet, -1);
		//System.out.println(Arrays.deepToString(cacheSet));
		return;
	}


	//takes in instruction line and translates
	public static void lineTranslation(String instruction) {
		String[] instr = instruction.split(" ");
		String command = instr[0];
		hexAdd = instr[1];
		currentAdd = addressTranslation(instr[1].substring(2));
		currentBlock = blockTranslation(currentAdd);
		byteLength = Integer.parseInt(instr[2]);
		currentSet = setTranslation(currentAdd);
		if (command.equals("store")) {
			newData = instr[3];
			store();
		} else {
			load();
		}
		return;
	}


	public static void main(String[] args) throws IOException{
		cacheSize = Integer.parseInt(args[1]);
		associativity = Integer.parseInt(args[2]);
		blockSize = Integer.parseInt(args[3]);
		makeCache();
		initializeRAM();
		BufferedReader br = new BufferedReader(new FileReader(new File(args[0])));
		String line = br.readLine();
		while (line != null) {
			lineTranslation(line);
			line = br.readLine();
		}
		return;
	}
}


