import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = readForCounts(in);
		
		HuffNode root = makeTree(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();
		
		
	}
	
	private int[] readForCounts(BitInputStream in) {
		int[] counts = new int[ALPH_SIZE + 1];
		while(true) {
			int index = in.readBits(BITS_PER_WORD);
			if (index == -1) {
				break;
			}
			counts[index]++;
		}
		counts[PSEUDO_EOF] = 1;
		return counts;
	}

	private String[] makeCodingsFromTree(HuffNode root) {
		String[] codings = new String[ALPH_SIZE+1];
		codingHelper(root, "", codings);
		return codings;
	}

	private HuffNode makeTree(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int i = 0; i< counts.length; i++) {
			if(counts[i]>0) {
				pq.add(new HuffNode(i,counts[i],null,null));
			}
		}
		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    HuffNode t = new HuffNode(0,left.myWeight+right.myWeight, left, right);
		    pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
		
	}
	
	private void codingHelper(HuffNode tree, String path, String[] codings) {
		if (tree.myLeft == null && tree.myRight == null) {
			codings[tree.myValue] = path;
			return;
		}
		
		else {
			codingHelper(tree.myLeft, path + "0", codings);
			codingHelper(tree.myRight, path + "1", codings);
		}
		
	}
	
	private void writeHeader(HuffNode tree, BitOutputStream out) {
		if (tree.myLeft == null && tree.myRight == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, tree.myValue);
		}
		else {
			out.writeBits(1, 0);
			writeHeader(tree.myLeft, out);
			writeHeader(tree.myRight, out);
		}
	}
	
	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {

		while (true) {
			int index = in.readBits(BITS_PER_WORD);

			if (index == -1) {
			  break;
			}

			String code = codings[index];
			out.writeBits(code.length(), Integer.parseInt(code, 2));

			}

		String code = codings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code, 2));

	}
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int magic = in.readBits(BITS_PER_INT);
		if (magic != HUFF_TREE|| magic == -1) {
			throw new HuffException("invalid magic number "+magic);
		}
		
		HuffNode root = readTree(in);
		HuffNode current = root;
		
		while (true){
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO EOF");
				
			}
			
			else {
				if(bits == 0) current = current.myLeft;
				else current = current.myRight;
				//is this the right code, myleft and myrights
				//shouldnt it just be left or right.
				
				if (current.myLeft== null && current.myRight==null) {
					if (current.myValue == PSEUDO_EOF)
						break;
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root;
					}
				}
				
			}
			
		}
		out.close();
		
		
		
		
	}
	
	private HuffNode readTree(BitInputStream in){
		int val = in.readBits(1); 
		if (val == -1) {
			throw new HuffException("ReadBits returned -1");
		}
		if (val == 0) {
			//idea.  the right tree is the one that that
			//always starts with the last 0 in the bit.
			//but how do i index the last zero.
			
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0,0,left,right);
			
		}
		else {
			int value = in.readBits(BITS_PER_WORD +1);
			return new HuffNode(value, 0, null, null);
			
		}
	
	}
}