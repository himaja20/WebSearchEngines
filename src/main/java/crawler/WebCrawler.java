package crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class WebCrawler {

	private final int MAX_PAGES = 50;
	
	@Parameter(names={"-url", "-u"},required = true)
	private String startingUrl;
	@Parameter(names={"-query", "-q"},required = true,variableArity = true)
	private List<String> query;
	@Parameter(names={"-docs"},required = true)
	private String docs;
	@Parameter(names={"-maxPages", "-m"})
	private int maxPages = MAX_PAGES;
	@Parameter(names={"-trace", "-t"})
	private boolean debug = false;
	
	private final String AGENT = "User-agent: *";
	private final String DISALLOW = "Disallow:";
	private HashMap<URL,Link> knownUrls;
	private PriorityQueue<Link> urlQueue;
	private int pagesHandled = 0;
	
	public static void main(String[] args) throws IOException {
		WebCrawler wc = new WebCrawler();
		new JCommander(wc, args);
		wc.run(args);	
	}

	public WebCrawler(){
		knownUrls = new HashMap<URL,Link>();
		urlQueue = new PriorityQueue<Link>(MAX_PAGES,new ScoreComparator());

	}

	public void run(String[] args) throws IOException{
		System.out.println("Crawling for " + maxPages+ " pages relevant to " + args[1] + " starting from " + startingUrl);
		System.out.println("\n");
		try{
			URL url = new URL(startingUrl);
			Link newLink = new Link(url,"");
			knownUrls.put(url, newLink);
			urlQueue.add(newLink);
		}
		catch(MalformedURLException e){
			System.out.println("Invalid starting URL " + args[0]);
		}

		while((!(urlQueue.size() == 0)) && pagesHandled < maxPages){

			Link newLink = urlQueue.poll();
			URL newUrl = newLink.getURL();
			if(debug){
				System.out.println("Downloading : "+ newLink.getURL() + " with score = " + newLink.getScore());
			}
			if (!robotSafe(newUrl)){
				continue;
			}
			File dFile = downloadFile(newUrl);
			if(dFile != null){
				if(debug){
					System.out.println("Received : "+ newUrl);
				}
				pagesHandled++;
				knownUrls.put(newUrl, newLink);
				fetchAnchorLinks(dFile, newUrl);
			}
		}   
	}

	public boolean robotSafe(URL url) throws IOException{
		String strHost = url.getHost();
		// form URL of the robots.txt file
		String strRobot = "https://" + strHost + "/robots.txt";
		URL urlRobot;
		try { urlRobot = new URL(strRobot);
		} catch (MalformedURLException e) {
			// something weird is happening, so don't trust it
			return false;
		}
		// reading the robots.txt
		String strCommands = null;
		String inputLine;
		BufferedReader br;
		try {
			URLConnection uConn = urlRobot.openConnection();
			br = new BufferedReader(
					new InputStreamReader(uConn.getInputStream()));
		} catch (IOException e) {
			// if there is no robots.txt file, it is OK to search
			return true;
		}

		while ((inputLine = br.readLine()) != null) {
			strCommands += inputLine;
		}

		// assume that this robots.txt refers to us and 
		// search for "Disallow:" commands.
		String strURL = url.getFile();
		int index = 0;
		int reqAgentIndex = strCommands.indexOf(AGENT);
		int nextAgentIndex = strCommands.indexOf("User-agent", reqAgentIndex);
		while ((index != -1) && (index < nextAgentIndex)){
			index = strCommands.indexOf(DISALLOW, reqAgentIndex);
			index += DISALLOW.length();
			String strPath = strCommands.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);

			if (!st.hasMoreTokens())
				break;

			String strBadPath = st.nextToken();

			// if the URL starts with a disallowed path, it is not safe
			if (strURL.indexOf(strBadPath) == 0)
				return false;
		}
		return true;
	}

	public void fetchAnchorLinks(File file, URL parentUrl) throws IOException{
		Document doc = Jsoup.parse(file,"UTF-8",parentUrl.toString());
		Elements links = doc.select("a[href]");
		for (Element link : links) {
			String linkHref = link.attr("href");
			String anchorText = link.text();

			URL childUrl = new URL(parentUrl,linkHref);
			Link newLink = new Link(childUrl,anchorText);
			int score = computeScore(file,link,parentUrl);
			if (pagesHandled == maxPages){
				break;
			}
			if(!knownUrls.containsKey(childUrl)){
				knownUrls.put(childUrl, newLink);
				if(!urlQueue.contains(newLink)){
					newLink.setScore(score);
					urlQueue.add(newLink);
					if(debug){
						System.out.println("Adding to queue: " + newLink.getURL() + " with score = " + newLink.getScore());
					}
				}
			}
			else{
				Link retrievedLink = knownUrls.get(childUrl);
				if(urlQueue.contains(retrievedLink) && score > 0){
					urlQueue.remove(retrievedLink);
					int newScore = score + retrievedLink.getScore();
					retrievedLink.setScore(newScore);
					urlQueue.add(retrievedLink);
					if(debug){
						System.out.println("Adding " + score + " to score of " + retrievedLink.getURL());
					}
				}
			}
		}
		System.out.println("\n");
	}

	public int computeScore(File file,Element link,URL parentUrl) throws IOException{
		if (query == null){
			return 0;
		}

		//substring
		String anchor = link.text().toLowerCase();
		//String[] queryTerms = query.split(" ");
		int K = 0;
		for (String q : query){
			if(anchor.contains(q)){
				K++;
			}
		}
		if (K > 0){
			return (K * 50);
		}

		//substring
		String url = link.attr("href").toLowerCase();
		K = 0;
		for (String q : query){
			if(url.contains(q)){
				return 40;
			}
		}
		
		int U = 0;
		int V = 0;
		List<String> neighborWords = new ArrayList<String>();

		List<String> words = getPrevNeighbors(link.previousSibling());
		if (words != null){
			neighborWords.addAll(words);
		}

		words = getNextNeighbors(link.nextSibling());
		if (words != null){
			neighborWords.addAll(words);
		}

		for(String q: query){
			if(neighborWords.contains(q)){
				U++;
			}
		}

		Document doc = Jsoup.parse(file, "UTF-8", parentUrl.toString());
		String rawText = doc.text();
		String[] raw = rawText.split(" ");
		List<String> rawTextList = new ArrayList<String>();
		for (String s : raw){
			if (!s.matches("^[a-zA-Z0-9]+$")){
				s = s.replaceAll("[^\\p{Alpha}\\p{Digit}]+","");
			}
			rawTextList.add(s.toLowerCase());
		}
		for(String q: query){

			if(rawTextList.contains(q)){
				V++;
			}
		}

		int score = 4*U + Math.abs(V-U);
		return score;


	}

	public List<String> getPrevNeighbors(Node prevSib){
		if (prevSib == null){
			return null;
		}

		String data;
		StringBuilder sb = new StringBuilder();
		String[] neighbors;
		List<String> retList = new ArrayList<String>();
		int count = 0;
		while (count < 5){
			data = getData(prevSib);
			if (data == null){
				break;
			}
			neighbors = data.split(" ");
			for (int i = neighbors.length - 1 ; i >= 0; i--){
				if (count == 5){
					break;
				}
				String word = neighbors[i];
				if(word.matches("^\\W+$") || word.matches("\\z")){
					continue;
				}
				if(!word.matches("^[a-zA-Z0-9]+$")){
					word = word.replaceAll("[^\\p{Alpha}\\p{Digit}]+","");
				}
				sb.append(word.toLowerCase());
				count++;
				sb.append(" ");
			}
			prevSib = prevSib.previousSibling();
		}
		for (String s : sb.toString().split(" ")){
			retList.add(s);
		}
		return retList;
	}

	public List<String> getNextNeighbors(Node nextSib){
		if (nextSib == null){
			return null;
		}

		String data;
		StringBuilder sb = new StringBuilder();
		String[] neighbors;
		List<String> retList = new ArrayList<String>();
		int count = 0;
		while (count < 5){
			data = getData(nextSib);
			if(data == null){
				break;
			}
			neighbors = data.split(" ");
			for (String s : neighbors){
				if (count == 5){
					break;
				}
				if(s.matches("^\\W+$") || (s.matches("\\z"))){
					continue;
				}
				if(!s.matches("^[a-zA-Z0-9]+$")){
					s = s.replaceAll("[^\\p{Alpha}\\p{Digit}]+","");
				}
				sb.append(s.toLowerCase());
				count++;
				sb.append(" ");
			}
			nextSib = nextSib.nextSibling();
		}
		for (String s : sb.toString().split(" ")){
			retList.add(s);
		}
		return retList;
	}

	public String getData(Node node){
		if (node == null){
			return null;
		}
		if (node instanceof TextNode){
			return ((TextNode)node).text();
		}
		return ((Element)node).text();
	}
	public File downloadFile(URL url) throws IOException{
		String inputLine;
		BufferedReader br = null;
		String fileName = docs + url.getHost()+ url.getPath();

		File f = new File(new File(fileName).getParent());
		f.mkdirs();

		File fileToSave = new File(docs + url.getHost() + url.getFile());
		if (!fileToSave.exists()){
			fileToSave.createNewFile();
		}
		FileWriter fw = new FileWriter(fileToSave.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		//StringBuilder sb = new StringBuilder();

		try {
			URLConnection uConn = url.openConnection();
			br = new BufferedReader(
					new InputStreamReader(uConn.getInputStream()));
			while ((inputLine = br.readLine()) != null) {
				bw.write(inputLine);
			}
		} catch (IOException e) {
			System.out.println("URL does not exist");
			return null;
		}
		finally{
			br.close();
			bw.close();
		}
		return fileToSave;
	}
}

