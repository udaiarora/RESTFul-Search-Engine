/*
Author: Udai Arora
Date: 2/14/2014
*/

package edu.asu.irs13;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

@Path("/home")
public class SearchFiles {
	//@SuppressWarnings({ "unchecked", "rawtypes", "rawtypes", "resource" })
	//Defining Public Variables
	static Map<String, Double> idf_map = new HashMap<String, Double>();
	static IndexReader r;
	static double mod_d_idf[];
	static double mod_d_tf[];
	static Term temp_term;
	static TermDocs temp_doc;
	static double mod_q=0; //Modulus of query
	static Map<Integer,Double> result;
	static double tfidf_similarity;
	static double tf_similarity;
	static Map<Integer, Double> result_tfidf_sorted;
	static String[] terms;
	static int term_count=366533;
	static double [][] doc_vocab_vector;
	static Map<Integer, Integer> top_n_docs;
	static Map<Integer, Integer> top_n_docs_reverse;
	static String[] stoplist= {"browserinfo", "target", "onmouseover", "menulink", "title","titles", "onmouseout", "hover", "onhover", "onmouseenter", "onmousein", "pts","px","margin","p7defmark", "helvetica", "index_files","projects_files",".gif", "en", "_blank", "height", "width", "alt", "jpg", "jpeg", "png", "sunsans","sans","serif","helvetica ", "arial", "font", "face", "ref", "left","right","middle","colspan","rowspan", "amp", "class", "name","value", "quot", "www.asu.edu", "href", "nbsp","http", "a","abbr","acronym","address","applet","area","article","aside","audio","b","base","basefont","bdi","bdo","big","blockquote","body","br","button","canvas","caption","center","cite","code","col","colgroup","datalist","dd","del","details","dfn","dialog","dir","div","dl","dt","em","embed","fieldset","figcaption","figure","font","footer","form","frame","frameset","h1","h2", "h3", "h4", "h5", "h6","head","header","hr","html","i","iframe","img","input","ins","kbd","keygen","label","legend","li","link","main","map","mark","menu","menuitem","meta","meter","nav","noframes","noscript","object","ol","optgroup","option","output","p","param","pre","progress","q","rp","rt","ruby","s","samp","script","section","select","small","source","span","strike","strong","style","sub","summary","sup","table","tbody","td","textarea","tfoot","th","thead","time","title","tr","track","tt","u","ul","var","video","wbr"};
	
	public static void main(String[] args){
		
	}
	@GET
	@Produces("text/plain")
	public String mainFunction() throws CorruptIndexException, IOException, JSONException{
	  	           
	                
		// the IndexReader object is the main handle that will give you all the documents, terms and inverted index
				r = IndexReader.open(FSDirectory.open(new File("C:\\Users\\Udai\\Desktop\\work\\IR_RestfulAPI\\index")));
			  	   
			 	// You can figure out the number of documents using the maxDoc() function
				System.out.println("The number of documents in this index is: " + r.maxDoc());
				
				
				//Start Timer
				long start_timer=startTimer();
				//Pre-computing Mod D and IDF
				System.out.println("Please wait while |D| is calculated...");
				mod_d_idf=new double[r.maxDoc()];
				mod_d_tf=new double[r.maxDoc()];
				TermEnum term_index= r.terms();
				double no_in_which_term_appears=0;
				while(term_index.next())
				{	
					
					temp_term = new Term("contents", term_index.term().text());
					temp_doc = r.termDocs(temp_term);

					//Update the IDF map
					no_in_which_term_appears=r.docFreq(temp_term);
					if (no_in_which_term_appears==0) {
						no_in_which_term_appears=r.maxDoc();
					}
					idf_map.put(term_index.term().text(), (double) Math.log(r.maxDoc()/no_in_which_term_appears));
					
					//Now for each doc in the term, increase the mod_d value by the square of the frequency of the term in that document
					
					while(temp_doc.next())
					{
						mod_d_tf[temp_doc.doc()]= (double) (mod_d_tf[temp_doc.doc()] + Math.pow((temp_doc.freq()),2));
						//System.out.println("yo");
						mod_d_idf[temp_doc.doc()]= (double) (mod_d_idf[temp_doc.doc()] + Math.pow((temp_doc.freq()*idf_map.get(term_index.term().text())),2));
						
					}
					
				}
				
				//The the square root to get the final mod_d value.
				int j=0;
				for(j=0;j<r.maxDoc();j++){
					mod_d_idf[j]=(double) Math.sqrt(mod_d_idf[j]);
					mod_d_tf[j]=(double) Math.sqrt(mod_d_tf[j]);
				}
				System.out.println("Done.");

				/* END PRECOMPUTING |D| */
				//End Timer
				endTimer("Time taken for pre-computations: ", start_timer);
				
				return "Done";
	}
	
	

	public static void tfidf(String str) throws IOException{
		//Call function to calculate IDF
		result = new HashMap<Integer, Double>();
		Map<String, Integer> query=new HashMap<String, Integer>();
		mod_q=0;
		tf_similarity=0;
		terms = str.split("\\s+");
		
		
		//Finding |Q|
		for(String w : terms)
		{
			if(query.containsKey(w)){
				query.put(w, (int)query.get(w)+1);
			}
			else {
				query.put(w, 1);
			}
		}
		
		
		Iterator it = query.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        mod_q=(int) (mod_q+Math.pow((int)pairs.getValue(),2));
	    }
	    mod_q=(double) Math.sqrt(mod_q);
		    //|Q| done

    	Map<Integer, Double> result=tfidf_calculator(terms);
	}

	
	
	
	@GET @Path("/tfidf")
	@Produces(MediaType.APPLICATION_JSON)
	
	public static JSONObject tfidfGet(@QueryParam("query") String str) throws JSONException, IOException {
		
		tfidf(str);
		
	    JSONArray ret_pr= new JSONArray();
	    int iterator=0; //To get just top few results with title and snippet. Rest wont have these attr.
		result_tfidf_sorted = sortByValue(result);
	    for(Map.Entry<Integer, Double> entry : result_tfidf_sorted.entrySet())
	    {	
	    	
	    	JSONObject objc = new JSONObject();
	    	int mapkey=entry.getKey();
	    	
	    	objc.put("id", mapkey);
	    	objc.put("sim", entry.getValue());
	    	
	    	Document d = r.document(mapkey);
			String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
			
			objc.put("url","http://"+url.replace("%%", "/"));
			objc.put("cachedurl","/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25"));
			
			String linkText="";
			String snipText="";
			if(iterator<200) {
				iterator++;
				linkText=titleandsnippet(str, url, mapkey)[0];
				snipText=titleandsnippet(str, url, mapkey)[1];
				
			}
			
			
	    	objc.put("title", linkText);
	    	objc.put("snippet", snipText);
			
	    	ret_pr.put(objc);
	    }
	    
	    JSONObject ret_obj = new JSONObject();
	    ret_obj.put("tfidf", ret_pr);
	    ret_obj.put("count", ret_pr.length());
	    
	    return ret_obj;
	   
	    
	}
	
	
	
	
	@GET @Path("/auth")
	@Produces(MediaType.APPLICATION_JSON)
	
	public static JSONObject authorHub(@QueryParam("query") String str) throws JSONException, IOException {
		
		tfidf(str);
		
		int[] base_set= baseSet(result);
	    
	    int[][] adj_mat= adjMat(base_set);

	    int[][] AProdAT= AProdATranspose(adj_mat);
	    int[][] ATProdA= ATransposeProdA(adj_mat);
	    
	    double [] last_auth= new double[base_set.length];
	    double [] last_hub= new double[base_set.length];
	    double[] this_auth;
	    double[] this_hub;
	    Map<Integer, Double> result_hub = new HashMap<Integer, Double>();
	    Map<Integer, Double> result_auth = new HashMap<Integer, Double>();
	    
	    for(int i=0; i<base_set.length; i++)
	    	last_hub[i]=last_auth[i]=1.0;
	    
	    //Power Iterations for Authority
	    while(true) {
	    	this_auth=powerIterate(last_auth, ATProdA);
	    	if(diff(last_auth, this_auth, 0.000001)){
	    		last_auth=this_auth;
	    	}
	    	else break;
	    }
	    
	    //Power Iterations for Hubness
	    while(true) {
	    	this_hub=powerIterate(last_hub, AProdAT);
	    	if(diff(last_hub, this_hub, 0.000001)){
	    		last_hub=this_hub;
	    	}
	    	else break;
	    }
	    
	    
	    // Putting the results into a Hash Map
	    Map<Integer, Integer> index_doc_maping= new HashMap<Integer, Integer>();
		for(int i=0;i<base_set.length; i++) {
			index_doc_maping.put(i, base_set[i]);
		}
	    for(int i=0;i<base_set.length; i++)
	    {
	    	result_auth.put(index_doc_maping.get(i), this_auth[i]);
	    	result_hub.put(index_doc_maping.get(i), this_hub[i]);
	    	
	    }
	    
	    
	    
	    Map<Integer, Double> result_auth_sorted= new HashMap<>();
	    Map<Integer, Double> result_hub_sorted= new HashMap<>();
	    result_auth_sorted=sortByValue(result_auth);
	    result_hub_sorted=sortByValue(result_hub);
	    
	    JSONArray ret_auth= new JSONArray();
	    JSONArray ret_hub= new JSONArray();
	    
	    for(Map.Entry<Integer, Double> entry : result_auth_sorted.entrySet())
	    {	JSONObject objc = new JSONObject();
    		int mapkey=entry.getKey();
	    	objc.put("id", mapkey);
	    	objc.put("sim", entry.getValue());
	    	Document d = r.document(mapkey);
			String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
			objc.put("url","http://"+url.replace("%%", "/"));
			objc.put("cachedurl","/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25"));

			String linkText=titleandsnippet(str, url, mapkey)[0];
			String snipText=titleandsnippet(str, url, mapkey)[1];
			
	    	objc.put("title", linkText);
	    	objc.put("snippet", snipText);
	    	
	    	ret_auth.put(objc);
	    }
	    long start_timer=startTimer();
		
	    for(Map.Entry<Integer, Double> entry : result_hub_sorted.entrySet())
	    {	JSONObject objc = new JSONObject();
    		int mapkey=entry.getKey();
	    	objc.put("id", mapkey);
	    	objc.put("sim", entry.getValue());
	    	Document d = r.document(mapkey);
			String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
			objc.put("url","http://"+url.replace("%%", "/"));
			objc.put("cachedurl","/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25"));
			
			String linkText=titleandsnippet(str, url, mapkey)[0];
			String snipText=titleandsnippet(str, url, mapkey)[1];
			
	    	objc.put("title", linkText);
	    	objc.put("snippet", snipText);
	    	
	    	ret_hub.put(objc);
	    }
	    endTimer("Time taken 200 snippets: ", start_timer);
		

	    JSONObject ret_obj = new JSONObject();
	    ret_obj.put("authorities", ret_auth);
	    ret_obj.put("hubs", ret_hub);
	    ret_obj.put("count", ret_auth.length());
	    return ret_obj;
	   
	}


	
	
	

	@GET @Path("/pagerank")
	@Produces(MediaType.APPLICATION_JSON)
	public static JSONObject pagerank(@QueryParam("query") String str) throws JSONException, IOException {
		
		
		tfidf(str);
			    
		double pagerank_weight=0.4;
	  	System.out.println("Calculating PageRanks");
    	double[] norm_adj_mat;
    	double [] last_pagerank=new double[r.maxDoc()];
    	double [] this_pagerank=new double[r.maxDoc()];

	    List<Integer> nolinks=new ArrayList<Integer>(); //Contains 
	    LinkAnalysis.numDocs = r.maxDoc();
		LinkAnalysis l = new LinkAnalysis();
		
    	for(int x=0; x<r.maxDoc();x++) {
    		last_pagerank[x]=1.0/(double)r.maxDoc();
    	}
    	for(int x=0; x<r.maxDoc();x++) {
    		this_pagerank[x]=0.0;
    	}
    	for(int i=0;i<r.maxDoc();i++) {
    		if(l.getLinks(i).length==0)
			nolinks.add(i);
		}
    	double temp=0;
    	
    	
    	int iter=1;
    	while(true) {
    		this_pagerank=new double[r.maxDoc()];
    		System.out.println("iteration "+ iter++);
    		//Calculating M*
	    	for(int i=0; i<r.maxDoc();i++) {
	    		norm_adj_mat=new double[r.maxDoc()];
	    		
	    		
	    		//Find M
	    		
	    		int[] arr2= l.getCitations (i);
	    		int[] links;
	    		
	    		
	    		for(int v : arr2) 
	    		{
	    			links=l.getLinks(v);
	    			norm_adj_mat[v]= (1.0/(double)links.length);
	    		}
	    		
	    		for(int v : nolinks)
	    		{
	    			norm_adj_mat[v]=(1.0/(double)r.maxDoc());
	    		}
	    		
	    		
	    		//Now find M*
	    		//Prob C=0.80
	    		for(int k=0; k<r.maxDoc();k++) {
	    			temp=(double) ((0.8*norm_adj_mat[k])+(0.2/(double)r.maxDoc()));
	    			this_pagerank[i]+=last_pagerank[k]*temp;
	    		}
	    		temp=0;
	    		
	    	}
	    	
	    	if(diff(last_pagerank, this_pagerank, 0.0001)){
	    		last_pagerank=this_pagerank;
	    	}
	    	else break;
	    }
    	
    	
    	System.out.println("Done.");
    	double[] normalized= new double[this_pagerank.length];
    	System.arraycopy(this_pagerank, 0, normalized, 0, this_pagerank.length);
    	Arrays.sort(normalized);
    	double min=normalized[0];
    	System.out.println("Min" + min);
    	double max=normalized[normalized.length-1];
    	System.out.println("Max" + max);
    	double min_max_diff=max-min;
    	Map<Integer, Double> result_pagerank_idf= new HashMap<Integer, Double>();
    	

	    System.out.println("Normalizing..");
    	Map<Integer, Double> result_pagerank= new HashMap<Integer, Double>();
	    for(int i=0;i<r.maxDoc(); i++)
	    {
	    	this_pagerank[i]=(this_pagerank[i]-min)/min_max_diff;
	    	result_pagerank.put(i, this_pagerank[i]);
	    	double idf_val;
	    	try {
	    		idf_val = result.get(i);
	    	}
	    	catch(Exception e) {
	    		continue;
	    	}
	    	result_pagerank_idf.put(i, (this_pagerank[i]*pagerank_weight)+(idf_val*(1-pagerank_weight)));
	    }
	    

	    Map<Integer, Double> result_pr_sorted = sortByValue(result_pagerank_idf);
	    JSONArray ret_pr= new JSONArray();
	    int iterator=0;
	    for(Map.Entry<Integer, Double> entry : result_pr_sorted.entrySet())
	    {	JSONObject objc = new JSONObject();
			int mapkey=entry.getKey();
			
	    	objc.put("id", mapkey);
	    	objc.put("sim", entry.getValue());
	    	
	    	Document d = r.document(mapkey);
			String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
		
			objc.put("url","http://"+url.replace("%%", "/"));
			objc.put("cachedurl","/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25"));

			String linkText="";
			String snipText="";
			if(iterator<500) {
				iterator++;
				linkText=titleandsnippet(str, url, mapkey)[0];
				snipText=titleandsnippet(str, url, mapkey)[1];
				
			}
			
	    	objc.put("title", linkText);
	    	objc.put("snippet", snipText);
	    	
	    	ret_pr.put(objc);
	    }
	    
	    JSONObject ret_obj = new JSONObject();
	    ret_obj.put("pagerank", ret_pr);
	    ret_obj.put("count", ret_pr.length());
	    return ret_obj;
	   
	    
	}
	
	
	

	@GET @Path("/cluster")
	@Produces(MediaType.APPLICATION_JSON)
	
	public static JSONObject cluster(@QueryParam("query") String str) throws IOException, JSONException {
		long start_timer=startTimer();
		
		tfidf(str);
		int k=3;
		int n=50;
		top_n_docs = new HashMap<Integer,Integer>();
		top_n_docs_reverse = new HashMap<Integer,Integer>();
		doc_vocab_vector = new double[n][term_count];
		String[] queryterms=str.split(" +");
		int doc_ind;
		double tf_clust;
		double idf_clust;
		String[][] cluster_sum_result = new String[k][5];
//		String highest_idf_term="";
//		String[] str_arr=str.split(" +");
//		for(int query_idf_looper=0; query_idf_looper<str_arr.length;query_idf_looper++) {
//			if(highestidf<idf_map.get(str_arr[query_idf_looper])) {
//				highest_idf_term=str_arr[query_idf_looper];
//			}
//		}
		//Getting top n docs 
		Map<Integer, Double> sorted_result = sortByValue(result);
		Iterator i = sorted_result.entrySet().iterator();
		int count=0;
		while(i.hasNext() && count<n){
			Integer key = (Integer) ((Entry) i.next()).getKey();
			top_n_docs.put(key, count);
			top_n_docs_reverse.put(count, key);
	        count++;
        }
		n=count;
		//Now for each term, loop over the documents and update the matrix when the document id is one of the top 50
		TermEnum term_index= r.terms();
		count=0;

		Map<String, Integer> term_mapping = new HashMap<String, Integer>();
		Map<Integer,String> reverse_term_mapping = new HashMap<Integer,String>();
		double[] centroid_magnitude_squared=new double[k];
		while(term_index.next())
		{	
			
			temp_term = new Term("contents", term_index.term().text());
			temp_doc = r.termDocs(temp_term);
			while(temp_doc.next())
			{
				
				if(top_n_docs.containsKey(temp_doc.doc())) {
					doc_ind=top_n_docs.get(temp_doc.doc());
							try{
								//tf_clust=r.docFreq(temp_term);
								tf_clust=temp_doc.freq();
								idf_clust=idf_map.get(term_index.term().text());
								doc_vocab_vector[doc_ind][count]=tf_clust*idf_clust;
								//
							}
							//
					catch(Exception e){
						
						System.out.println( count +"Threw err" +doc_ind);
						break;
					}
				}
			}
				term_mapping.put(term_index.term().text(),count);
				reverse_term_mapping.put(count, term_index.term().text());
				count++;
			
		}
		
		
		//Centroid vector
		double centroids[][] = new double[k][term_count];
		Queue<Integer> clusters[]= new Queue[k];
	
		Random random = new Random();
		//Randomly chosing 3 docs as seed
		for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
			for(int term_looper=0; term_looper<term_count; term_looper++) {
				centroids[cluster_looper][term_looper]= doc_vocab_vector[cluster_looper][term_looper];
			}
		}
		
		
		double curr_sim=0.0;
		int curr_cluster=0;
		Map<Integer, Double> cl_sum= new HashMap<Integer, Double>();
		while(true) {
			// Initializing queue to be empty
			double mod_centroid[]=new double[k];
			//Array of Priority Queues to store docs in respective clusters
			//The L2 norm of clusters is also calculated
			for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
				clusters[cluster_looper]=new PriorityQueue<Integer>();
				for(int term_looper=0; term_looper<term_count; term_looper++) {
					mod_centroid[cluster_looper]+=Math.pow(centroids[cluster_looper][term_looper],2);
				}
				mod_centroid[cluster_looper]=Math.sqrt(mod_centroid[cluster_looper]);
			}
			
			//Find similarity between the docs and the centroids so as to decide which cluster to put the doc
			for(int doc_looper=0; doc_looper<n; doc_looper++) {
				double max=0;
				for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
					for(int term_looper=0; term_looper<term_count; term_looper++) {
						curr_sim+=centroids[cluster_looper][term_looper]*doc_vocab_vector[doc_looper][term_looper];
					}
					curr_sim/=mod_centroid[cluster_looper];
					if(curr_sim>max) {
						max=curr_sim;
						curr_cluster=cluster_looper;
					}
				}
				clusters[curr_cluster].add(doc_looper);
			}
			
			//Evaluate new centroids.
			double new_centroids[][] = new double[k][term_count];
			
			for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
				int no_of_docs_in_cluster=0;
				for(int d : clusters[cluster_looper]) {
					for(int term_looper=0; term_looper<term_count; term_looper++) {
						new_centroids[cluster_looper][term_looper]+=doc_vocab_vector[d][term_looper];
					}
					no_of_docs_in_cluster++;
				}
				
				//dividing each term in the centroid by no of documents to get the avg
				if(no_of_docs_in_cluster==0) no_of_docs_in_cluster=1;
				for(int term_looper=0; term_looper<term_count; term_looper++) {
					new_centroids[cluster_looper][term_looper]/=no_of_docs_in_cluster;
				}
				
			}
			boolean loop_again=false;
			//Compare with old centroids to check if the loop is to run again.
			for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
				for(int term_looper=0; term_looper<term_count; term_looper++) {
					 if(Math.abs(centroids[cluster_looper][term_looper]-new_centroids[cluster_looper][term_looper])!=0) {
						 loop_again=true;
					 }
				}
			}
			
			if(loop_again==false){
				for(int cluster_looper=0; cluster_looper<k; cluster_looper++){

					Map<Integer, Double> cluster_sum= new HashMap<Integer, Double>();
					for(int term_looper=0; term_looper<term_count; term_looper++) {
						centroid_magnitude_squared[cluster_looper]+=Math.pow(centroids[cluster_looper][term_looper], 2);
						cluster_sum.put(term_looper,centroids[cluster_looper][term_looper] );
					}
					Map<Integer, Double> sorted_cs= sortByValue(cluster_sum);
					
					
				    int ccc=0;
				    JSONArray suggestions=new JSONArray();
				    String regex = "^[a-zA-Z]+$";
				    
					for(Map.Entry<Integer, Double> entry : sorted_cs.entrySet())
				    {	
						
						boolean che=Arrays.asList(stoplist).contains((reverse_term_mapping.get(entry.getKey())));
						boolean che2= Arrays.asList(queryterms).contains((reverse_term_mapping.get(entry.getKey())));
						if(che || che2 || !reverse_term_mapping.get(entry.getKey()).matches(regex))
						{
							continue;
						}
						
						boolean skip=false;
						for(int cl_loop=cluster_looper; cl_loop>=0;cl_loop--){
							if(Arrays.asList(cluster_sum_result[cl_loop]).contains(reverse_term_mapping.get(entry.getKey())))
								{
									skip=true;
									break;
								}
						}
						if(skip==true) continue;
						
						cluster_sum_result[cluster_looper][ccc]=reverse_term_mapping.get(entry.getKey());
						//System.out.println(cluster_sum_result[cluster_looper][ccc]+" ");
						if(ccc==4)
							break;
						ccc++;
				    }
					
					
					
					
				}
				break;
			}
			
			//Centroids=new Centroids
			for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
				System.arraycopy(new_centroids[cluster_looper], 0, centroids[cluster_looper], 0, centroids[cluster_looper].length);
			}
		}
		
		//Print clusters
//		for(int cluster_looper=0; cluster_looper<k; cluster_looper++){
//			System.out.println("cluster"+cluster_looper+": ");
//			 while (clusters[cluster_looper].size() != 0){
//				 System.out.println(top_n_docs_reverse.get(clusters[cluster_looper].remove()));
//			}
//		}
		
		endTimer("Time taken for clustering: ", start_timer);
		
	    JSONArray ret_pr= new JSONArray();

		for(int cluster_looper=0; cluster_looper<k; cluster_looper++)
	    {	
			System.out.println("Cluster magnitude squared for cluster "+cluster_looper+" is "+centroid_magnitude_squared[cluster_looper]);
			JSONObject objc = new JSONObject();
	    	JSONArray obja= new JSONArray();
	    	JSONObject objc2 = null;
	    	while(clusters[cluster_looper].size()!=0)
	    		{
	    			objc2 = new JSONObject();
	    			int mapkey=top_n_docs_reverse.get(clusters[cluster_looper].remove());
	    			objc2.put("id",mapkey);
	    	    	Document d = r.document(mapkey);
	    			String url = d.getFieldable("path").stringValue(); // the 'path' field of the Document object holds the URL
	    			objc2.put("url","http://"+url.replace("%%", "/"));
	    			objc2.put("cachedurl","/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25"));

	    			String linkText=titleandsnippet(str, url, mapkey)[0];
	    			String snipText=titleandsnippet(str, url, mapkey)[1];
	    			
	    	    	objc2.put("title", linkText);
	    	    	objc2.put("snippet", snipText);
	    	    	
	    	    	obja.put(objc2);
	    		}
	    	objc.put("cluster", obja);
	    	String summary=new String();
	    	for(int rl=0;rl<5;rl++) {
	    		summary=summary+cluster_sum_result[cluster_looper][rl]+" ";
	    	}
	    	objc.put("summary", summary);
	    	objc.put("count", obja.length());
	    	ret_pr.put(objc);
		}
		
	    JSONObject ret_obj = new JSONObject();
	    ret_obj.put("clusters", ret_pr);
	    ret_obj.put("count", ret_pr.length());
	    return ret_obj;
		
	}
	
	
	
	

	@GET @Path("/sugg")
	@Produces(MediaType.APPLICATION_JSON)
	public static JSONObject expandQuery(@QueryParam("query") String str) throws IOException, JSONException {
		tfidf(str);
		TermEnum term_index= r.terms();
		int doc_ind;
		int n=10;

		top_n_docs = new HashMap<Integer,Integer>();
		top_n_docs_reverse = new HashMap<Integer,Integer>();
		doc_vocab_vector = new double[n][12000];
		
		//Top n documents
		Map<Integer, Double> sorted_result = sortByValue(result);
		Iterator i = sorted_result.entrySet().iterator();
		int count=0;
		while(i.hasNext() && count<n){
			Integer key = (Integer) ((Entry) i.next()).getKey();
			top_n_docs.put(key, count);
			top_n_docs_reverse.put(count, key);
	        count++;
        }

		n=count;
		
		//Create doc-term matrix
		
		count=0;
		Map<String, Integer> term_mapping = new HashMap<String, Integer>();
		Map<Integer,String> reverse_term_mapping = new HashMap<Integer,String>();
		while(term_index.next())
		{	
			double term_idf=idf_map.get(term_index.term().text());
			boolean no_docs=true;
			temp_term = new Term("contents", term_index.term().text());
			temp_doc = r.termDocs(temp_term);
			while(temp_doc.next())
			{
				if(top_n_docs.containsKey(temp_doc.doc()) && term_idf>0.2) {
					no_docs=false;
					doc_ind=top_n_docs.get(temp_doc.doc());
							try{
								//tf_clust=r.docFreq(temp_term);
								doc_vocab_vector[doc_ind][count]=temp_doc.freq();
								//
							}
							//
					catch(Exception e){
						
						System.out.println( count +"Threw err" +doc_ind);
						break;
					}
				}
			}
			if(!no_docs){
				term_mapping.put(term_index.term().text(),count);
				reverse_term_mapping.put(count, term_index.term().text());
				count++;
			}
		}
	
		int doc_vocab_len0=doc_vocab_vector[0].length;
		double docTermpProdTermDoc[][]=TDprodDT(doc_vocab_vector);
		double NormalizeddocTermpProdTermDoc[][]=new double[doc_vocab_len0][doc_vocab_len0];
		//Normalizing
		for(int row_looper=0; row_looper<doc_vocab_len0; row_looper++) {
			for(int col_looper=0; col_looper<doc_vocab_len0; col_looper++) {
				NormalizeddocTermpProdTermDoc[row_looper][col_looper]=(docTermpProdTermDoc[row_looper][col_looper])/(docTermpProdTermDoc[row_looper][row_looper]+docTermpProdTermDoc[col_looper][col_looper]-docTermpProdTermDoc[row_looper][col_looper]);
			}
		}
		

		Map<Integer,Double> simresults=new HashMap<Integer,Double>();
		String[] queryterms=str.split(" +");
		for(String qt : queryterms) {
			for(int cc=0; cc<doc_vocab_len0; cc++) {
				if(simresults.containsKey(cc)){
					simresults.put(cc, simresults.get(cc)+NormalizeddocTermpProdTermDoc[term_mapping.get(qt)][cc] );
				}
				else {
					try{simresults.put(cc, NormalizeddocTermpProdTermDoc[term_mapping.get(qt)][cc] );}
				catch(Exception e) {
					System.out.println("Error in scalar."+e);
				}
				}
			}
		}

	    Map<Integer, Double> result_qr_sorted = sortByValue(simresults);
	    int ccc=0;
	    JSONArray suggestions=new JSONArray();
	    String regex = "^[a-zA-Z]+$";
	    
		for(Map.Entry<Integer, Double> entry : result_qr_sorted.entrySet())
	    {	
			if(ccc<queryterms.length )
			{
				ccc++;
				continue;
			}
			boolean che=Arrays.asList(stoplist).contains((reverse_term_mapping.get(entry.getKey())));
			if(che || !reverse_term_mapping.get(entry.getKey()).matches(regex))
			{
				continue;
			}
			//System.out.println((reverse_term_mapping.get(entry.getKey())));
			suggestions.put((reverse_term_mapping.get(entry.getKey())));
			if(ccc==50+queryterms.length)
				break;
			ccc++;
	    }
		System.out.println("DONE");
		JSONObject ret_obj= new JSONObject();
	    ret_obj.put("sug", suggestions);
	    return ret_obj;
		
	}
	
	


	@GET @Path("/querycomp")
	@Produces(MediaType.APPLICATION_JSON)
	public static JSONArray queryCompletion(@QueryParam("partialquery") String str2) throws IOException, JSONException {
		String str=str2.split(" +")[str2.split(" +").length-1];
		System.out.println("Called Query Completion");
		JSONArray ret_arr= new JSONArray();
		String reg="^"+str+".*$";
		String regex2= "^[a-zA-Z]+$";
		int counter=0;
		for(Entry<String, Double> entry : idf_map.entrySet())
	    {	
			if(entry.getKey().matches(reg) && entry.getKey().matches(regex2)) {
				String putstring="";
				for(int st=0;st<str2.split(" +").length-1;st++)
				{
					putstring=putstring+str2.split(" +")[st]+" ";
				}
				putstring=putstring+entry.getKey();
				counter++;
				JSONObject ret_obj= new JSONObject();
				ret_obj.put("value", putstring);
				ret_arr.put(ret_obj);
				//System.out.println( entry.getKey());
			}
			else {
				continue;
			}
			if(counter==5) {
				break;
			}
	    }
		return ret_arr;
	}
	
	
	public static double[][] TDprodDT(double[][] adj_mat) {
		double[][] AT= new double[adj_mat[0].length][adj_mat.length];
		double[][] AProdAT= new double[adj_mat[0].length][adj_mat[0].length];
		for(int i = 0;i<adj_mat[0].length;i++) {
			for(int j =0;j<adj_mat.length;j++) {
				AT[i][j]=adj_mat[j][i];
			}
		}
		
		
		for (int i=0; i<adj_mat[0].length; i++)
	    {
			for (int j=0; j<adj_mat[0].length; j++)
		    
	    	{
		    	AProdAT[i][j]=0;
		    	for (int k=0; k<adj_mat.length; k++)
		    	{
		    		AProdAT[i][j] += AT[i][k] * adj_mat[k][j];
		    	}
	    	}
	    }
		    	
		return AProdAT;
	}
	
	
	
	
	
	public static String[] titleandsnippet(String str, String url, int mapkey) {
		String linkText = null;
		String forsnipText = null;
		String snipText = null;
		String[] st = null;
		try{
			org.jsoup.nodes.Document docu = Jsoup.connect("http://localhost:8080/IR_RestfulAPI/Projectclass/result3/"+url.replace("%%", "%25%25")).get();
			Elements newsHeadlines = docu.getElementsByTag("title");
			Elements snip = docu.getElementsByTag("html");
			linkText = newsHeadlines.text();
			forsnipText = snip.text();
			forsnipText=forsnipText.toLowerCase();
			st=forsnipText.split(" +");
			String[] strsplit=str.split(" +");
			boolean flag=true;
			for (int snip_looper = 0; snip_looper < st.length; snip_looper++) 
			{
				for (int query_looper = 0; query_looper < strsplit.length; query_looper++) 
				{
					if (st[snip_looper].contains(strsplit[query_looper])) {
						snipText="";
						for(int snip_word_looper=11; snip_word_looper>0;snip_word_looper--)
						{
						snipText+=(snip_looper-snip_word_looper > 0 ? st[snip_looper-snip_word_looper]+" " : "");
						}
						snipText+= "<b>"+st[snip_looper]+"</b>";
                        for(int snip_word_looper=1; snip_word_looper<11;snip_word_looper++)
						{
                        	snipText+=(snip_word_looper < st.length ? " "+st[snip_looper+snip_word_looper] : "") ;
						}
						snipText+=(snip_looper+10 < st.length ? " "+"..." : "");
						flag=false;
						break;
					}
				}
				if(flag==false) break;
			}
		}
		catch(Exception e) {
			System.out.println("Error on "+ mapkey);
		}
		
		String titleandsnip[] = new String[2];
		titleandsnip[0]=linkText;
		titleandsnip[1]=snipText;
		return titleandsnip;
	}
	
	
	
	
	//Function to calculate tf-idf similarity
	public static Map<Integer, Double> tfidf_calculator(String[] terms) throws IOException{
		
		double idf=0;
		
		for(String word : terms)
		{
			Term term = new Term("contents", word);
			TermDocs tdocs = r.termDocs(term);
			while(tdocs.next())
			{
				idf= (double) idf_map.get(word);
				
				tfidf_similarity=(double)tdocs.freq()* idf /((double)mod_q * (double)mod_d_idf[tdocs.doc()]); //where tdocs.freq=tf
				
				if(result.containsKey(tdocs.doc()))
				{
					//result.get(tdocs.doc());
					result.put(tdocs.doc(), ((double)result.get(tdocs.doc())) + tfidf_similarity);
				}
				else {
					result.put(tdocs.doc(), tfidf_similarity);
				}
			}
		}

		long start_timer=startTimer();
        Map<Integer, Double> sorted_result = sortByValue(result);
        endTimer("Time taken to sort the results: ", start_timer);
        return sorted_result;

	}
	
	//Function to find the base set
	public static int[] baseSet(Map final_result) {
		LinkAnalysis.numDocs = r.maxDoc();
		LinkAnalysis l = new LinkAnalysis();
		Iterator i = final_result.entrySet().iterator(); 
		int count=1;
		Set<Integer> base_set= new TreeSet<Integer>();
		while(i.hasNext() && count<11){
			Integer key = (Integer) ((Entry) i.next()).getKey();
			base_set.add(key);
			//Added this to baseSet
			int[] links = l.getLinks(key);
			int[] citations = l.getCitations(key);
			for (int li: links) {
				base_set.add(li);
			}
			for (int ci: citations) {
				base_set.add(ci);
			}
            count++;  
        }
		int[] base=new int[base_set.size()];
		int index=0;
		for (Integer integer : base_set)
			base[index++]=integer;
		return base;
	}
	
	//Function to find the adjacency matrix
	public static int[][] adjMat(int[] base_set) {
		LinkAnalysis.numDocs = r.maxDoc();
		LinkAnalysis l = new LinkAnalysis();
		int [][] adj= new int[base_set.length][base_set.length];
		Map<Integer, Integer> index_doc_maping= new HashMap<Integer, Integer>();
		for(int i=0;i<base_set.length; i++) {
			index_doc_maping.put(i, base_set[i]);
		}
		//Explicit Type casting not possible for arrays of int to Integer.
		
		for(int i=0;i<base_set.length;i++) 
		{
			int[] arr2= l.getLinks(index_doc_maping.get(i));
			Integer[] arr = new Integer[arr2.length];
			int count = 0;
			for(int a : arr2){
				arr[count++] = a;
			}
			
			for(int j=0;j<base_set.length;j++) 
			{
				//System.out.println(index_doc_maping.get(j) + " " + Arrays.asList(arr).contains((Integer)index_doc_maping.get(j)));
				if(Arrays.asList(arr).contains(index_doc_maping.get(j))) 
				{
					adj[i][j]=1;
				}
				
				else
					adj[i][j]=0;
			}
		}
		return adj;
	}


	
	
	public static int[][] AProdATranspose(int[][] adj_mat) {
		int[][] AT= new int[adj_mat.length][adj_mat.length];
		int[][] AProdAT= new int[adj_mat.length][adj_mat.length];
		for(int i = 0;i<adj_mat.length;i++) {
			for(int j =0;j<adj_mat.length;j++) {
				AT[i][j]=adj_mat[j][i];
			}
		}
		
		
		for (int i=0; i<adj_mat.length; i++)
		    for (int j=0; j<adj_mat.length; j++)
		    	{
		    	AProdAT[i][j]=0;
		    	for (int k=0; k<adj_mat.length; k++)
			    	  AProdAT[i][j] += adj_mat[i][k] * AT[k][j];
		    	}
		    	
		return AProdAT;
	}
	
	
	public static int[][] ATransposeProdA(int[][] adj_mat) {
		int[][] AT= new int[adj_mat.length][adj_mat.length];
		int[][] ATProdA= new int[adj_mat.length][adj_mat.length];
		for(int i =0;i<adj_mat.length;i++) {
			for(int j =0;j<adj_mat.length;j++) {
				AT[i][j]=adj_mat[j][i];
			}
		}
		
		
		for (int i=0; i<adj_mat.length; i++)
		    for (int j=0; j<adj_mat.length; j++)
		    	{
		    	ATProdA[i][j]=0;
		    	for (int k=0; k<adj_mat.length; k++)
			    	  ATProdA[i][j] += AT[i][k] * adj_mat[k][j];
		    	}
		    	
		return ATProdA;
	}
	
	//Power ITeration for Auth and Hubs
	public static double[] powerIterate(double[] last_auth, int[][] aTProdA) {
		double[] this_auth= new double[last_auth.length];
		for(int i=0;i<last_auth.length;i++) {
			this_auth[i]=0.0;
			for(int j=0; j<last_auth.length;j++) {
				this_auth[i]+=aTProdA[i][j] * last_auth[j];
			}
		}
		double sum_of_sq=0.0;
		//Normalization index
		for(int i=0; i<this_auth.length;i++) {
			sum_of_sq+=this_auth[i]*this_auth[i];
		}
		sum_of_sq=Math.sqrt(sum_of_sq);
		//Normalizing
		for(int i=0; i<this_auth.length;i++) {
			this_auth[i]/=sum_of_sq;
		}
		
		return this_auth;
	}
	
	//To check if diff in 2 arays is less than a given value
	public static boolean diff(double[] last_auth, double[] this_auth, double d) {
		for(int i=0;i<last_auth.length;i++) {
			if(Math.abs(this_auth[i]-last_auth[i])>d)
				return true;
		}
		return false;
	}

	
	//Function to print the resulting hash map similarity
	public static void printResult(Map result) throws CorruptIndexException, IOException{
		//System.out.println(result);
		Iterator i = result.entrySet().iterator(); 
		int count=1;
		
			
		while(i.hasNext() && count<11){
			Integer key = (Integer) ((Entry) i.next()).getKey();
			System.out.println(key + " : " + result.get(key));
			//System.out.println(key);
	            count++;
            
        }
	}
	
	
	//Defining Functions for measuring time
	public static long startTimer(){
		return(System.nanoTime());
	}
	
	public static void endTimer(String S, long start_time){
		long duration=  System.nanoTime()-start_time;
		System.out.println(S + duration/1000000.0 + " milli-seconds"); 
	}
		
	//Function which inputs a hash map, sorts it and returns it.
	@SuppressWarnings("rawtypes")
	public static <Key extends Comparable, Value extends Comparable> Map<Key,Value> sortByValue(Map<Key,Value> map){
		
		List<Map.Entry<Key,Value>> obj = new LinkedList<Map.Entry<Key,Value>>(map.entrySet());
		
	    Collections.sort(obj, new Comparator<Map.Entry<Key,Value>>() {
	        @SuppressWarnings("unchecked")
			@Override
	        public int compare(Entry<Key, Value> first_entry, Entry<Key, Value> second_entry) {
	            return second_entry.getValue().compareTo(first_entry.getValue());
	        }
	    });
	  
	    Map<Key,Value> sortMap = new LinkedHashMap<Key,Value>();
	  
	    for(Map.Entry<Key,Value> entry: obj){
	        sortMap.put(entry.getKey(), entry.getValue());
	    }
	  
	    return sortMap;
	}
	
	

	

	
}
