import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CS143bProj1 {
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
static class PCB {
	String pid;
	int priority;
	int[] resources; int[] needs;
	String statType; //ready,running,blocked
	int statList; //blocked=0,1,2,3  ready=4
	PCB parent; ArrayList<PCB> children;
	PCB(String name,int prio){
		pid = name;  priority = prio;
		resources = new int[4]; needs = new int[4];
		statType = "ready"; statList = 4;
		children = new ArrayList<PCB>();
		if(processes.size()>0)
			parent = running;//current last process				
	}
}
static ArrayList<PCB> processes = new ArrayList<PCB>();
static ArrayList<ArrayList<PCB>> readyLists   = new ArrayList<ArrayList<PCB>>(); //prio 0,1,2
static ArrayList<ArrayList<PCB>> blockedLists = new ArrayList<ArrayList<PCB>>(); //R1,2,3,4
static int[] resources = {1,2,3,4};
static PCB running;
static String toWrite = "";
//privates~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
private static void printArray(ArrayList<PCB> processes,String whichList) {
	System.out.print(whichList+"[");
	for (PCB p:processes)
		System.out.print(p.pid+",");
	System.out.println("]");
}
//printArray(processes,"processes");
//printArray(readyLists.get(2),"ready2");//		printArray(readyLists.get(1),"ready1");
//printArray(blockedLists.get(list),"blocked"+(list+1));//		printArray(aList,"aList");
private static int find(ArrayList<PCB> processes,String name) {//finds a process given its name
	for (int i=0; i<processes.size(); i++) {
		if (processes.get(i).pid.equals(name))
			return i;
	}return 0;
}
private static int findMax() {//finds the array index the highest,first process resides
	int max = 0;
	for(int i=2;i>0;i--) {
		if(max<i && readyLists.get(i).size()>0)
			max = i;
	}return max;
}
private static int findRList(String s) {//finds the list given by req/rel
	int list = 4;
		 if(s.equals("R1")) {list=0;} else if(s.equals("R2")) {list=1;}
	else if(s.equals("R3")) {list=2;} else if(s.equals("R4")) {list=3;}
	return list;
}
private static void realloc(int list) {//unblocks top blocked if possible
	int avail = resources[list];
	while(blockedLists.get(list).size()>0 && blockedLists.get(list).get(0).needs[list]<=avail) {
//		there's enough to unblock
		PCB transfer = blockedLists.get(list).get(0);
		int need = transfer.needs[list]; //how many transfer needs to be unblocked
		transfer.resources[list] += need;  resources[list] -= need;
		transfer.statType = "ready";  transfer.statList = 4;
		blockedLists.get(list).remove(0);
		readyLists.get(transfer.priority).add(transfer);
	}
}
private static ArrayList<PCB> addList(PCB delete){
	ArrayList<PCB> aList = new ArrayList<PCB>();
	aList.add(delete);
	ArrayList<PCB> bList;	
	for(PCB i : delete.children) {
		bList = addList(i);
		for(PCB j: bList)
			aList.add(j);
	}
	return aList;
}
//project methods~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
static boolean Create(String name,int prio){
	if(prio==0 && name!="init") //only "init 0"; no "init 1" or "x 0"
		return false;
	if(prio<0 || prio>2 || find(processes,name)>0) //wrong priority or exists
		return false;
	PCB newProcess = new PCB(name,prio);
	if(processes.size()>0) //make parent (if exists) have newProcess as child
		running.children.add(newProcess);
	processes.add(newProcess);
	readyLists.get(prio).add(newProcess);
	Scheduler();
	return true;
}
static boolean Destroy(String name){
	int delete = find(processes,name);
	if (delete == 0)//not exists
		return false;
	PCB isRunning = processes.get(delete);
	while(true) {//check if running is either isRunning or one of it's ancestors
		if(running==isRunning)
			break;
		else if(isRunning.parent != null)
			isRunning = isRunning.parent;
		else
			return false;//non-descendant, therefore can't call destroy
	}
//	add toDelete and children-tree to arrayList deleteList
	PCB paramDelete = processes.get(delete);
	paramDelete.parent.children.remove(paramDelete);//have parent remove paramDelete
	ArrayList<PCB> deleteList = addList(paramDelete);
	for(PCB toDelete : deleteList) {//begin removing objects in deleteList
		for(int i=0;i<4;i++){
			if(toDelete.resources[i]>0)//any resources to reallocate
				resources[i] += toDelete.resources[i];
		}
		if(toDelete.statList==4){
			readyLists.get(toDelete.priority).remove(toDelete);
			if(toDelete.statType=="running") {
				running = null;
			}
		}
		if(toDelete.statType=="blocked"){
			blockedLists.get(toDelete.statList).remove(toDelete);
		}
		processes.remove(toDelete);
	}
	for(int i=0;i<4;i++)
		realloc(i);
	Scheduler();
	return true;
}
static boolean Request(String rid,int n){
	int list = findRList(rid);
	if(list>3){return false;}//Not R1-4
	if(processes.size()==1){return false;}//only init
	if(list+1<n){return false;}//asking for too much
	if(running.resources[list]+n>list+1) {return false;}//requesting twice in a row
	if(n==0){return true;}//requesting 0 terminates w/o error to prevent blocked list for 0
	if(resources[list]>=n){//transfer resources if enough
		running.resources[list] += n;
		resources[list] -= n;
	}else{//set blocked if not enough
		running.needs[list] = n;
		running.statType = "blocked"; running.statList = list;
		readyLists.get(running.priority).remove(running);
		blockedLists.get(list).add(running);
		running = null;
	}
	Scheduler();
	return true;
}
static boolean Release(String rid,int n){
	int list = findRList(rid);
	if(list>3){return false;}//Not R1-4
	if(list+1<n){return false;}//trying to release too much
	if(n==0){return true;}//releasing 0 terminates w/o error
	if(running.resources[list]<n){return false;}//trying to release nonexisting resources
	running.resources[list] -= n;
	resources[list] += n;
	realloc(list);
	Scheduler();
	return true;
}
static void Timeout(){
	int max = findMax();
	PCB topSwap = readyLists.get(max).get(0);
	topSwap.statType = "ready"; running = null;
	readyLists.get(max).remove(0);
	readyLists.get(max).add(topSwap);
	Scheduler();
}
static void Init(){
	processes    = new ArrayList<PCB>();
	readyLists   = new ArrayList<ArrayList<PCB>>(); //prio 0,1,2
	blockedLists = new ArrayList<ArrayList<PCB>>(); //R1,2,3,4
	for(int i=0;i<3;i++) {readyLists.add(new ArrayList<PCB>());}
    for(int i=0;i<4;i++) {blockedLists.add(new ArrayList<PCB>());}
	resources[0]=1;resources[1]=2;resources[2]=3;resources[3]=4;
	running = null;
	toWrite += "\n";
	Create("init",0);
}
private static void Scheduler(){
	int max = findMax();
	PCB top = readyLists.get(max).get(0);
	if(running!=top){
		top.statType = "running";//set ready->running and running->ready
		if(running!=null && running.statType.equals("running"))
			running.statType = "ready";
		running = top;//take current running and turn to ready
	}
	toWrite += running.pid+" ";
}
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
public static void main(String[] args) throws IOException {
	FileReader fr = new FileReader("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 1\\input.txt");
//	FileReader fr = new FileReader("D:\\input.txt");
	BufferedReader br = new BufferedReader(fr);
//	File outFile = new File("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 1\\60599867.txt"); outFile.createNewFile(); FileWriter fw = new FileWriter(outFile);
	FileWriter fw = new FileWriter("C:\\Users\\Colin\\Documents\\UCI_4th_year\\CS 143b\\Proj 1\\60599867.txt");
//	FileWriter fw = new FileWriter("D:\\60599867.txt");
    String preline=null; String[] line; boolean result;
	for(int i=0;i<3;i++) {readyLists.add(new ArrayList<PCB>());}
    for(int i=0;i<4;i++) {blockedLists.add(new ArrayList<PCB>());}
//    Begin
	Create("init",0);
	fw.write(toWrite);
	while(true) {
		toWrite = "";
		result = true;
//		read command
		preline = br.readLine();
    	if (preline==null)//reached end of file
    		break;
    	line = preline.split(" ");
//		do command
		if (line.length == 1){//to, init
//			System.out.println(line[0]);
			if(line[0].equals("")) {/*System.out.println("empty\n");*/}
			else if(line[0].equals("to"))
				Timeout();
			else if(line[0].equals("init"))
				Init();
			else
				result = false;
		}
		else if (line.length==2 && line[0].equals("de") && line[1].length()==1){//de x
//			System.out.println(line[0]+" "+line[1]);
			result = Destroy(line[1]);
		}
		else if (line.length == 3){//cr, req, rel
//			System.out.println(line[0]+" "+line[1]+" "+line[2]);
			if(line[0].equals("cr") && line[1].length()==1)//assumes line[2] is int
				result = Create(line[1],Integer.parseInt(line[2]));
			else if(line[0].equals("req") && line[1].length()==2){//assumes line[2] is int
				result = Request(line[1],Integer.parseInt(line[2]));
			}
			else if(line[0].equals("rel") && line[1].length()==2){//assumes line[2] is int
				result = Release(line[1],Integer.parseInt(line[2]));
			}
			else
				result = false;
		}
		else
			result = false;
		if(result==false)
			toWrite += "error ";
		fw.write(toWrite);
	}//end while
	fr.close(); fw.flush();fw.close();
}//end main
}