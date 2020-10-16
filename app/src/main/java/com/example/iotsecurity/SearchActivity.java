package com.example.iotsecurity;

import android.bluetooth.BluetoothClass;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

public class SearchActivity<i> extends AppCompatActivity {
    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        View rootView = inflater.inflate(R.layout.activity_search2, container, false);
//
//        TextView tv = rootView.findViewById(R.id.tv);
//        try {
//            readResourceInfo();
//            readDeviceInfo();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return rootView;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search2);

        // 현재 저장되어 있는 제품의 data
        Product product = (Product)getIntent().getExtras().get("product");
        Vector<Double> productVector = getProductVector(product);

        TextView tv = findViewById(R.id.tv);
        try {
            readResourceInfo();
            readDeviceInfo();
            devices.get(0);
            CalculateCos(productVector, devices);
            ArrayList<Product> similarProducts = top5Print(devices);
            Log.d("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@", devices.get(0).getName());
            makeProductRecyclerView(similarProducts);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Problem ver 2020.10.16 : devices에서는 순위가 제대로 나오지만 top5List로 옮기면 제대로 나오지 않음.
     * -> 참조에서 생기는 문제이므로 메소드 구조(파라미터, 반환값, void 등)를 변경할 필요 있음
     *
     *
     * 제품 데이터를 받아서 리사이클러 뷰로 출력
     * 리사이클러뷰에는 클릭 리스너
     * 아이템 클릭시 해당 리소스를 현재 Product내용과 병합 (병합할 때 제품 정보에 비어있는 값만 채워 넣음)
     * 병합 후에는 DB에 저장
     * 이 저장된 Product 데이터는 ProductFragment에서 출력.
     * @param similarProducts recycler view로 띄울 제품에 대한 데이터가 담겨있는 리스트
     */
    private void makeProductRecyclerView(ArrayList<Product> similarProducts) {
        for(int i=0; i<similarProducts.size(); i++) {
            Log.d("!!@!@!@!@!@!@!@!@!@@!@!@!!@!@ " , similarProducts.get(i).category);

        }
    }

    private Vector<Double> getProductVector(Product product) {
        Vector<Double> productVec = new Vector<Double>(13);
        if(product.provider.equals("Philips"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        if(product.category.equals("전구"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        if(product.category.equals("조명"))
            productVec.addElement((Double)1.0);
        else
            productVec.addElement((Double)0.0);

        // 장치 이동성 true:이동형, fasle:고정형
        if(product.portable) {
            productVec.addElement((Double)0.0);
            productVec.addElement((Double)1.0);
        }
        else {
            productVec.addElement((Double)1.0);
            productVec.addElement((Double)0.0);
        }


        //연결 방식 wifi, bluetooth, z-wave 순서
        if(product.connection.contains("wifi"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.connection.contains("bluetooth"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.connection.contains("z-wave"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);

        //디바이스 타입 : 스마트장비, 센서, 액츄에이터 순서
        if(product.deviceType.contains("스마트장비"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.deviceType.contains("센서"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);
        if(product.deviceType.contains("액츄에이터"))
            productVec.addElement((Double) 1.0);
        else
            productVec.addElement((Double)0.0);

        // 디스플레이 유무 : 있음, 없음 순서
        if(product.display) {
            productVec.addElement((Double) 1.0);
            productVec.addElement((Double) 0.0);
        }
        else {
            productVec.addElement((Double) 0.0);
            productVec.addElement((Double) 1.0);
        }
        return productVec;
    }

    //csv파일을 읽어서 devices에 정보 생성
    private ArrayList<Resource> resources = new ArrayList<Resource>();
    private void readResourceInfo() throws IOException {
        InputStream is  = getResources().openRawResource(R.raw.resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("EUC-KR")));
        String line="";
        try {
            while ((line = reader.readLine()) != null) {
                //Split by ','
                int idx = 0;
                String[] tokens = line.split(",");
                Resource sample = new Resource();
                //제품명 얻기
                sample.setIdx(idx++);
                sample.setName(tokens[0]);
                sample.setCategory(tokens[1]);
                sample.setManufactureName(tokens[2]);
                sample.setMovement(tokens[3]);
                sample.setConnection(tokens[4]);
                sample.setGatheringMethod(tokens[5]);
                sample.setServices(tokens[6]);
                sample.setDataList(tokens[7]);
                sample.setDataType(tokens[8]);
                sample.setAgreement(Integer.parseInt(tokens[9]));
                sample.setDeviceType(tokens[10]);
                sample.setDisplay(Integer.parseInt(tokens[11]));
                sample.setRiskScore(Double.parseDouble(tokens[12]));
                resources.add(sample);
//                Log.d("SearchActivity", "Just created Resource: "+sample);
            }
        } catch (IOException e){
            Log.wtf("Search Activity", "error reading data file on line" + line,e);
        }
    }

    //csv파일을 읽어서 devices에 정보 생성
    private ArrayList<DeviceInfo2> devices = new ArrayList<DeviceInfo2>();
    private void readDeviceInfo() throws IOException {
        InputStream is  = getResources().openRawResource(R.raw.hue);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("EUC-KR")));
        String line="";
        try {
            while ((line = reader.readLine()) != null) {
                //Split by ','
                int idx = 0;
                String[] tokens = line.split(",");
                DeviceInfo2 sample = new DeviceInfo2();
                //제품명 얻기
                sample.setIdx(idx++);
                sample.setName(tokens[0]);
                //데이터 구축
                //벡터 생성
                Vector<Double> vector = new Vector<Double>(13);
                vector.addElement(Double.parseDouble(tokens[1]));
                vector.addElement(Double.parseDouble(tokens[2]));
                vector.addElement(Double.parseDouble(tokens[3]));
                vector.addElement(Double.parseDouble(tokens[4]));
                vector.addElement(Double.parseDouble(tokens[5]));
                vector.addElement(Double.parseDouble(tokens[6]));
                vector.addElement(Double.parseDouble(tokens[7]));
                vector.addElement(Double.parseDouble(tokens[8]));
                vector.addElement(Double.parseDouble(tokens[9]));
                vector.addElement(Double.parseDouble(tokens[10]));
                vector.addElement(Double.parseDouble(tokens[11]));
                vector.addElement(Double.parseDouble(tokens[12]));
                vector.addElement(Double.parseDouble(tokens[13]));
                sample.setVector(vector);
                devices.add(sample);
//                Log.d("SearchActivity", "Just created: "+sample);
            }
        } catch (IOException e){
            Log.wtf("Search Activity", "error reading data file on line" + line,e);
        }
    }
    //코사인 유사도 계산 함수 input 벡터(사용자가 선택한 속성들을 벡터로 변환한 값 v1과 v2 우리가 구축한 devices에 있는 벡터값 유사도 계산)
    private double getScore(Vector<Double> v1, Vector<Double> v2) throws Exception{
        int v1Size = v1.size();
        if (v1Size != v2.size()){
            throw new Exception("Vectors not same size");
        }
        double numerator = 0;
        double v1squaresum = 0;
        double v2squaresum = 0;
        for (int i = 0; i < v1Size; i++){
            double v1Val = v1.get(i);
            double v2Val = v2.get(i);
            numerator += (v1Val * v2Val);
            v1squaresum += (v1Val * v1Val);
            v2squaresum += (v2Val * v2Val);
        }
        if (numerator == 0 || v1squaresum == 0 || v2squaresum == 0){
            return 0;
        }
        double denom = (Math.sqrt(v1squaresum) * Math.sqrt(v2squaresum));
        return numerator / denom;
    }

    //사용자 입력 벡터와 조사 데이터의 유사도계산
    private void CalculateCos(Vector<Double> vec, ArrayList<DeviceInfo2> devices){
        for(DeviceInfo2 di : devices){
            double score = 0;
            try {
                score = getScore(vec, di.getVector());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //DeviceInfo에 유사도 점수 입력
            di.setSimScore(score);
        }
        //유사도가 높은 장치를 맨위로 하여 정렬 수행
        Collections.sort(devices);
    }
    //정렬된 리스트를 사용하여 top5 장치 출력
    private ArrayList<Product> top5Print( ArrayList<DeviceInfo2> deviceInfo2s){
        //리스트에 뿌려주는 내용 필요
        ArrayList<Product> top5 = new ArrayList<Product>();
        for(int i=0; i<5; i++){
            top5.add(getIdxDPD(deviceInfo2s.get(i).getIdx()));
        }
        //리스트에서 해당 idx에 대한 클릭을 얻었을 때, 해당 productList의 해당 값의 idx를 얻어서 등록과정 종료
        //devices.get(productList.getIdx("사용자 선택 값"));

        return top5;
    }
    //만약 어떠한 값이 선택되었다고 하면 idx를 기반으로 원본 데이터 객체를 가져옴
    private Product getIdxDPD(int idx){
        Resource r = resources.get(idx);
        Product p = new Product();
        r.getIdx();
        p.name = r.getName();
        p.category = r.getCategory();
        p.provider = r.getManufactureName();
        p.portable = r.getMovement().contains("이동형");
        p.connection = r.getConnection();
        if(r.getGatheringMethod().contains("상시수집"))
            p.always = 3;
        else if(r.getGatheringMethod().contains("조건수집"))
            p.always = 2;
        else
            p.always = 1;
        p.serviceType = r.getServices();
        p.infoType = r.getDataType();
        p.agree = (r.getAgreement()==1);
        p.display = (r.getDisplay()==1);
        p.score = r.getRiskScore();

        Log.d("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", p.name);
        return p;
    }
}