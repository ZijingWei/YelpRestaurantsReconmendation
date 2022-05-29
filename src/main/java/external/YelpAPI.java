package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.cj.xdevapi.JsonValue;

import entity.Item;
import entity.Item.ItemBuilder;

public class YelpAPI {
	private static final String URL = "https://api.yelp.com/v3/businesses/search";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "Bearer g98-elDpasQm2Z21U3XzJdFyPrrarbcpZL-tvkvxisSU9YCQbm2vulL-6T_enqcry98oJsk8gFrSLBVEE_HPyOhEDgVNGwX9NCw_Pjvqn4ZH0AkjdTDS0zSueJCQYnYx";
	
	public List<Item> search(double lat, double lon, String catagory) {
		if (catagory == null) {
			catagory= DEFAULT_KEYWORD;
		}
		try {
			catagory= java.net.URLEncoder.encode(catagory, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Make your url query part like: "apikey=12345&geoPoint=abcd&keyword=music&radius=50"
		String query = String.format("latitude=%s&longitude=%s&categories=%s", lat, lon, catagory);
		
		try {
			// Open a HTTP connection between your Java application and TicketMaster based on url
			HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
			// Set request method to GET
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Authorization", API_KEY);
			// Send request to TicketMaster and get response, response code could be
			// returned directly
			// response body is saved in InputStream of connection.
			int responseCode = connection.getResponseCode();
			
			System.out.println("\nSending 'GET' request to URL : " + URL + "?" + query);
			System.out.println("Response Code : " + responseCode);
			
			// Now read response body to get events data
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
			JSONObject obj = new JSONObject(response.toString());
			if (obj.isNull("businesses")) {
				return new ArrayList<>();
			}
			JSONArray businesses = obj.getJSONArray("businesses");
			return getItemList(businesses);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();


	}
	
	
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null);
		try {
			for (Item item : itemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("location")) {
			JSONObject location = event.getJSONObject("location");
			JSONArray display = location.getJSONArray ("display_address");
			String result = "";
			for (int i = 0; i < display.length(); ++i) {
				result +=  display.getString(i) + " ";
			}
			if (!result.equals("")) {
				return result;
			}
		}

		return "";
	}

	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("image_url")) {
			return event.getString("image_url");
		}

		return "";
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull("categories")) {
			JSONArray classifications = event.getJSONArray("categories");
			for (int i = 0; i < classifications.length(); i++) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("title")) {
					String name = classification.getString("title");
					categories.add(name);
				}
			}
		}

		return categories;
	}

	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();

		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			
			builder.setCategories(getCategories(event));
			builder.setAddress(getAddress(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}

		return itemList;
	}

	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		YelpAPI tmApi = new YelpAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(37.786882, -122.399972);
	}
}
