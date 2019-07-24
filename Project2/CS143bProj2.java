import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CS143bproj2{

static int[] PhysMem = new int[524288]; //1024 frames, each 512 ints long
static boolean[] bitmap = new boolean[1024]; //indicates if a frame in PhysMem is free
static int[][] TLB = new int[4][2]; //saves recent memory searches to speed up process

static void printArray(int[] list) {
	System.out.print("[");
	for (int p:list){ System.out.print(p+","); }
	System.out.println("]");}
static void update_tlb(int start) {
	int sp = TLB[start][0]; int f = TLB[start][1];
	for(int i=start; i<3; i++) {
		TLB[i][0] = TLB[i+1][0];
		TLB[i][1] = TLB[i+1][1];
	}
	TLB[3][0] = sp; TLB[3][1] = f;
}
static int binary_to_decimal(int[] bin) {
	int dec = 0;
	int last_i = bin.length-1;
	for(int i=0; i<bin.length; i++)
		dec += Math.floor(Math.pow(2, last_i-i)) * bin[i];
	return dec;
}
static int[] decimal_to_binary(int va) {
	int[] binary = new int[32];
	int i = 31;
	for(int r=va; r>0; r/=2) {
		binary[i--] = r%2;
	}
	return binary;
}

public static void main(String[]args) throws IOException {
//	FileReader fr = new FileReader("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 2\\input1.txt");
	FileReader fr = new FileReader("D:\\input1.txt");
	BufferedReader br = new BufferedReader(fr);
    String preline=br.readLine();
	String[] line1 = preline.split(" ");		preline = br.readLine();
	String[] line2 = preline.split(" ");		preline = br.readLine();
    fr.close();
    for(int i=0; i<line1.length; i+=2) { //line1
    	int Sn = Integer.parseInt(line1[i]);
    	int Fn = Integer.parseInt(line1[i+1]);
    	PhysMem[Sn] = Fn;
    	if(Fn > 0) {
    		bitmap[Fn/512] = true;
    		bitmap[Fn/512+1] = true;
    	}
    }
    for(int i=0; i<line2.length; i+=3) { //line2
    	int Pm = Integer.parseInt(line2[i]);
    	int Sm = Integer.parseInt(line2[i+1]);
    	int Fm = Integer.parseInt(line2[i+2]);
    	PhysMem[PhysMem[Sm]+Pm] = Fm;
    	if(Fm > 0)
    		bitmap[Fm/512] = true;
    }
//	fr = new FileReader("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 2\\input2.txt");
	fr = new FileReader("D:\\input2.txt");
	br = new BufferedReader(fr);
	preline=br.readLine();		line1 = preline.split(" ");
	fr.close();
//	FileWriter fw = new FileWriter("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 2\\605998671.txt"); boolean run_tlb = false;
//	FileWriter fw = new FileWriter("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 2\\605998672.txt"); boolean run_tlb = true;
	FileWriter fw = new FileWriter("D:\\605998671.txt"); boolean run_tlb = false;
//	FileWriter fw = new FileWriter("D:\\605998672.txt"); boolean run_tlb = true;
    for(int i=0; i<line1.length; i+=2) {
    	int rw = Integer.parseInt(line1[i]);
    	int dec = Integer.parseInt(line1[i+1]); //System.out.println("rw: "+rw+", dec: "+dec);
    	int[] binary_VA = decimal_to_binary(dec); //printArray(binary_VA);
    	int s = binary_to_decimal(Arrays.copyOfRange(binary_VA,4,13)); //System.out.println("s: "+s+", PM[s]: "+PhysMem[s]);
    	int p = binary_to_decimal(Arrays.copyOfRange(binary_VA,13,23)); //System.out.println("p: "+p+", PM[PM[s]+p]: "+PhysMem[PhysMem[s]+p]);
    	int sp = binary_to_decimal(Arrays.copyOfRange(binary_VA,4,23)); //System.out.println("sp: "+sp);
    	int w = binary_to_decimal(Arrays.copyOfRange(binary_VA,23,32)); //System.out.println("w: "+w+", PM[PM[s]+p]+w: "+(PhysMem[PhysMem[s]+p]+w));
    	boolean in_tlb = false;
    	if(run_tlb) {
	    	for(int j=0; j<4; j++) {
	    		if (PhysMem[s]!=0 && PhysMem[PhysMem[s]+p]!=0 && TLB[j][0]==sp && TLB[j][1]==PhysMem[PhysMem[s]+p] ) {
	    			fw.write("h "); //System.out.println("h");
	    			fw.write(TLB[j][1]+w+" "); //System.out.println(TLB[j][1]+w);
	    			update_tlb(j);
	    			in_tlb = true;
	    			break;
	    		}
	    	}
    	}
    	if(run_tlb && !in_tlb)
    		fw.write("m "); //System.out.println("m");
    	if (rw==1 && PhysMem[s]==0 && !in_tlb){ //allocate new PT
    		for(int j=1; j<1023; j++) {//1023 to prevent out-of-range
    			if(bitmap[j]==true && bitmap[j+1]==true)
    				j++;
    			else if(bitmap[j]==false && bitmap[j+1]==false) {
					PhysMem[s] = j*512;
					bitmap[j]=true;
					bitmap[j+1]=true;
					break;
    			}
    		}
    	}
    	if (rw==1 && PhysMem[PhysMem[s]+p]==0 && !in_tlb){ //allocate new page
    		for(int j=1; j<1024; j++) {
    			if(bitmap[j]==false) {
    				PhysMem[PhysMem[s]+p] = j*512;
					bitmap[j]=true;
					fw.write(PhysMem[PhysMem[s]+p]+w+" "); //System.out.println(PhysMem[PhysMem[s]+p]+w);
		        	if(run_tlb) {
		        		TLB[0][0] = sp; TLB[0][1] = PhysMem[PhysMem[s]+p];
		        		update_tlb(0);
		        	}
					break;
    			}
    		}
    	}
    	else if (PhysMem[s]==-1 || PhysMem[PhysMem[s]+p]==-1) 
    		fw.write("pf "); //System.out.println("pf");
    	else if (rw==0 && PhysMem[s]==0 || rw==0 && PhysMem[PhysMem[s]+p]==0) 
    		fw.write("err "); //System.out.println("err");
    	else if(!in_tlb) {
    		fw.write(PhysMem[PhysMem[s]+p]+w+" "); //System.out.println(PhysMem[PhysMem[s]+p]+w);
    		if(run_tlb) {
        		TLB[0][0] = sp; TLB[0][1] = PhysMem[PhysMem[s]+p];
        		update_tlb(0);
        	}
    	}System.out.println("yee");
    }
	fw.flush(); fw.close();
}//end main
}