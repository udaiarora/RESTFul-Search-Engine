//Javascript for index.html


$(function() {
	function log( message ) {
		$( "<div>" ).text( message ).prependTo( "#log" );
		$( "#log" ).scrollTop( 0 );
	}
	
	$( "#query" ).autocomplete({
		source:  function(req, add){
			
                //pass request to server
                $.getJSON("/IR_RestfulAPI/rest/home/querycomp?partialquery="+$("#query").val(), req, function(data) {
                	
                    //create array for response objects
                    var suggestions = [];
                    
                    //process response
                    for(i=0; i<5;i++){                             
                    	suggestions.push(data[i].value);
                    }
                    
         	       //pass array to callback
         	       add(suggestions);
         	   });
                
            }
        });
});


function showAll(){
	$("#search").prop('disabled', false);
	$("#selectedtype").prop('disabled', false);
	$("#query").prop('disabled', false);
}

function hideAll(){
	$("#search").prop('disabled', true);
	$("#selectedtype").prop('disabled', true);
	$("#query").prop('disabled', true);
}


$.ajax({
	url: "/IR_RestfulAPI/rest/home",
	success: function() {
		$(".loader").fadeOut(250);
		$(".heading").css("margin-top","200px");
		$(".search-area input").focus();
	}

});


$(document).ready(function(){
	var type;
	var getresponse;
	var leng;



	$(".search-area input").one("keydown",function(){
		$(".heading img").attr("width","150px");
		$(".heading").css({"margin-top":"20px"});
	});

	$("#search").on("click", function() {
		hideAll();
		$(".loading-results").css("display","block");
		type= document.getElementById("selectedtype").selectedIndex;
		var url_to_hit;

		switch(type) {
			case 1: url_to_hit="/IR_RestfulAPI/rest/home/auth";
			break;
			case 2: url_to_hit="/IR_RestfulAPI/rest/home/pagerank";
			break;
			case 3: url_to_hit="/IR_RestfulAPI/rest/home/cluster";
			break;
			default: url_to_hit="/IR_RestfulAPI/rest/home/tfidf";
		}


		$.ajax({
			url: url_to_hit,
			data: {
				query: $("#query").val()
			},
			success: function(response) {
				console.log(response);
				getresponse=response;
				$(".loading-results").css("display","none");
				$(".result-container").html(" ");
				$(".pagination").html(" ");
				$(".sugg").html('<span>Try adding these words: </span>');
				$(".sugg").append(" <img src='/IR_RestfulAPI/assets/img/loading.gif' alt='loading' height='15px'>");
				leng2=response.count;
				leng=response.count;
				till=Math.ceil(leng/10)>10?10:Math.ceil(leng/10);
				for (var i = 1; i <=till ; i++) {
					$(".pagination").append('<li><a class="paginated_button" href="#">'+i+'</a></li>');
				};
				leng=leng>10?10:leng;
				switch(type) {
					case 1: 
					for(i=0; i<leng; i++) {
						$(".result-container").append("<div class='col-lg-8 row result-item'><div class='col-lg-6'><div class='result-title'><a href='"+response.authorities[i].url+"'>"+response.authorities[i].id+"- "+response.authorities[i].title+"</a></div> - <a href='"+response.authorities[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+response.authorities[i].snippet+"</span></div><div class='col-lg-6'><div class='result-title'><a href='"+response.hubs[i].url+"'>"+response.hubs[i].id+"- "+response.hubs[i].title+"</a></div> - <a href='"+response.hubs[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+response.hubs[i].snippet+"</span></div></div>")
					}
					break;
					case 2: 
					for(i=0; i<leng; i++) {
						$(".result-container").append("<div class='col-lg-4 result-item'><div class='result-title'><a href='"+response.pagerank[i].url+"'>"+response.pagerank[i].id+"- "+response.pagerank[i].title+"</a></div> - <a href='"+response.pagerank[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+response.pagerank[i].snippet+"</span></div>")
					}
					break;
					case 3: 
					$(".pagination").html(" ");
					result_to_append="";
					//result_to_append+=("<div class='row'>");
					for(i=0; i<leng; i++) {
						result_to_append+=("<div class='col-lg-3'>Cluster "+parseInt(i+1)+"<br/>Key: "+response.clusters[i].summary)
						for (var j = 0; j < response.clusters[i].count; j++) {
							result_to_append+=("<div class='result-item'><div class='result-title'><a href='"+response.clusters[i].cluster[j].url+"'>"+response.clusters[i].cluster[j].id+"- "+response.clusters[i].cluster[j].title+"</a></div> - <a href='"+response.clusters[i].cluster[j].cachedurl+"'>Cached</a><br/><span class='snip'>"+response.clusters[i].cluster[j].snippet+"</span></div>")
						}
						result_to_append+=("</div>");
					}
					$(".result-container").append(result_to_append);
					break;
					default: 
					for(i=0; i<leng; i++) {
						$(".result-container").append("<div class='col-lg-4 result-item'><div class='result-title'><a href='"+response.tfidf[i].url+"'>"+response.tfidf[i].id+"- "+response.tfidf[i].title+"</a></div> - <a href='"+response.tfidf[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+response.tfidf[i].snippet+"</span></div>")
					}
				}

				$.ajax({
					url: "/IR_RestfulAPI/rest/home/sugg",
					data: {
						query: $("#query").val()
					},
					success: function(response) {
						console.log(response);
						showAll();
						$(".sugg").html('<span>Try adding these words: </span>');
						for(i=0;i<5;i++){
							$(".sugg").append("<a class='suggestion' href='#'>"+response.sug[i]+"</a>; ");
						}
					}
				})

			}

		});
});

$(".pagination").on("click", ".paginated_button", function(){
	$(".result-container").html(" ");	
	pagenum=$(this).text();

	$(".pagination").html(" ");
	if(pagenum>4){
		till=Math.ceil(leng2/10)>parseInt(pagenum)+5?parseInt(pagenum)+5:Math.ceil(leng2/10);
		for (var i = pagenum-4; i <=till ; i++) {
			$(".pagination").append('<li><a class="paginated_button" href="#">'+i+'</a></li>');
		};
	}
	else
	{
		till=Math.ceil(leng2/10)>10?10:Math.ceil(leng2/10);
		for (var i = 1; i <=till ; i++) {
			$(".pagination").append('<li><a class="paginated_button" href="#">'+i+'</a></li>');
		};
	}

	pagenum-=1;
	switch(type) {
		case 1: 
		for(i=pagenum*10; i<pagenum*10+10; i++) {
			$(".result-container").append("<div class='col-lg-8 row result-item'><div class='col-lg-6'><div class='result-title'><a href='"+getresponse.authorities[i].url+"'>"+getresponse.authorities[i].id+"- "+getresponse.authorities[i].title+"</a></div> - <a href='"+getresponse.authorities[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+getresponse.authorities[i].snippet+"</span></div><div class='col-lg-6'><div class='result-title'><a href='"+getresponse.hubs[i].url+"'>"+getresponse.hubs[i].id+"- "+getresponse.hubs[i].title+"</a></div> - <a href='"+getresponse.hubs[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+getresponse.hubs[i].snippet+"</span></div></div>")
		}
		break;
		case 2: 
		for(i=pagenum*10; i<pagenum*10+10; i++) {
			$(".result-container").append("<div class='col-lg-4 result-item'><div class='result-title'><a href='"+getresponse.pagerank[i].url+"'>"+getresponse.pagerank[i].id+"- "+getresponse.pagerank[i].title+"</a></div> - <a href='"+getresponse.pagerank[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+getresponse.pagerank[i].snippet+"</span></div>")
		}
		break;
		default: 
		for(i=pagenum*10; i<pagenum*10+10; i++) {
			$(".result-container").append("<div class='col-lg-4 result-item'><div class='result-title'><a href='"+getresponse.tfidf[i].url+"'>"+getresponse.tfidf[i].id+"- "+getresponse.tfidf[i].title+"</a></div> - <a href='"+getresponse.tfidf[i].cachedurl+"'>Cached</a><br/><span class='snip'>"+getresponse.tfidf[i].snippet+"</span></div>")
		}
	}
});

$(".sugg").on("click", ".suggestion", function(){
	console.log($("#query").val()+" "+$(this).html());
	$("#query").val($("#query").val()+" "+$(this).html());
	$("#search").trigger("click");
});

});
