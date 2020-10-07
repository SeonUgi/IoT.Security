package com.example.iotsecurity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 장치 정보를 받아와서 recyclerView로 장치별로 나눔.
 *
 * ver. 2020.09.26 : http request를 통해 장치 리스트를 json으로 받아옴.
 * 추후 db의 정보를 받아와서 json으로 재정의 후 정보 나누는 것으로 변경
 */
public class ProductFragment extends Fragment {

    ProductAdapter adapter;
    RecyclerView recyclerView;

    Product light;
    static RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.product_fragment, container, false);

        adapter = new ProductAdapter();

        recyclerView= rootView.findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Product tempLight = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable("product", tempLight);
                Intent intent = new Intent(getActivity(), ProductDetail.class);
                intent.putExtra("product", tempLight);

                startActivity(intent);
            }
        });

        requestQueue = Volley.newRequestQueue(getContext().getApplicationContext());
        String baseUrl = String.format("http://192.168.0.13/api/CNvVAzMQxpTl2FNN12ipOCvqxbA7X0HEbMoGXoht/lights/");
        makeRequest(baseUrl);

        return rootView;
    }

    private void makeRequest(String baseUrl){
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, baseUrl,null,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    processResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        });
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void processResponse(JSONObject response) throws JSONException {
        JSONObject lightJson;
        // response.lenth()가 1이 작게 나옴! 왜?!?!
        for (int i = 1; i<response.length()+1; i++) {
            lightJson = response.getJSONObject(String.valueOf(i));
            light = new Product();
            light.name = lightJson.getString("name");
            light.provider = lightJson.getString("manufacturername");
            light.category = "lights";
            light.score = 27.34;
            adapter.addItem(light);
        }

        adapter.notifyDataSetChanged();
    }

}
