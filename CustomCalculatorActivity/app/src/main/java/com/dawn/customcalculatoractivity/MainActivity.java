package com.dawn.customcalculatoractivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListView listViewResult;
    private EditText mInput;
    private LinearLayout display;
    private ImageView close, deleteAll;
    private Button[] buttons;//按钮

    private int screen_width;
    private int screen_height;
    private HashMap<View, String> map; // 将View和String映射起来
    private List<InputItem> mInputList; // 定义记录每次输入的数
    private int mLastInputStatus = INPUT_NUMBER; // 记录上一次输入状态
    public static final int INPUT_NUMBER = 1;//数字
    public static final int INPUT_POINT = 0;//点
    private static final int INPUT_OPERATOR = -1;//运算符
    public static final int END = -2;
    public static final int ERROR = -3;
    public static final String INPUT_ERROR = "錯誤";

    //***************************历史记录*********************************
    private final String FILENAME = "calculator_history";
    private StringBuilder historySB = new StringBuilder();//要保存的历史记录
    private StringBuilder hisHistorySB = new StringBuilder();//获取已保存的历史记录
    private String calHistory, hisHistory;
    private boolean isFirstOpenHistory = true;
    private String[] hisItemArray;
    private List<String> equationList = new ArrayList<String>();
    private EquationAdapter eAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        //初始化计算器键盘
        buttons = new Button[19];

        eAdapter = new EquationAdapter(getApplicationContext(), R.layout.history_item, equationList);
        listViewResult = (ListView) findViewById(R.id.listView);
        listViewResult.setAdapter(eAdapter);
        mInput = (EditText) findViewById(R.id.text2);
        display = (LinearLayout) findViewById(R.id.display);
        close = (ImageView) findViewById(R.id.close);
        deleteAll = (ImageView) findViewById(R.id.deleteAll);

        buttons[0] = (Button) findViewById(R.id.zero);
        buttons[1] = (Button) findViewById(R.id.one);
        buttons[2] = (Button) findViewById(R.id.two);
        buttons[3] = (Button) findViewById(R.id.three);
        buttons[4] = (Button) findViewById(R.id.four);
        buttons[5] = (Button) findViewById(R.id.five);
        buttons[6] = (Button) findViewById(R.id.six);
        buttons[7] = (Button) findViewById(R.id.seven);
        buttons[8] = (Button) findViewById(R.id.eight);
        buttons[9] = (Button) findViewById(R.id.nine);

        buttons[10] = (Button) findViewById(R.id.divide);//“÷”
        buttons[11] = (Button) findViewById(R.id.multiple);//“×”
        buttons[12] = (Button) findViewById(R.id.minus);//“-”
        buttons[13] = (Button) findViewById(R.id.plus);//“+”
        buttons[14] = (Button) findViewById(R.id.dis);//“Dis”
        buttons[15] = (Button) findViewById(R.id.dot);//“.”
        buttons[16] = (Button) findViewById(R.id.equal);//“=”
        buttons[17] = (Button) findViewById(R.id.empty);//“C”
        buttons[18] = (Button) findViewById(R.id.delete);//“←”

        setOnClickListener();// 调用监听事件
    }

    private void initData() {
        if (map == null)
            map = new HashMap<View, String>();
        for (int i = 0; i < 17; i++) {
            map.put(buttons[i], buttons[i].getText().toString());
        }
        mInputList = new ArrayList<InputItem>();
        clearInputScreen();
        showHistory();//显示历史记录
    }

    private void setOnClickListener() {
        close.setOnClickListener(this);
        deleteAll.setOnClickListener(this);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close:
                finish();
                break;
            case R.id.deleteAll:
                hisHistorySB.setLength(0);
                equationList.clear();
                eAdapter.notifyDataSetChanged();
                break;
            case R.id.delete://"←"
                back();
                break;
            case R.id.empty://"C"
                clearInputScreen();
                break;
            case R.id.equal://等号
                operatorResult();
                break;
            case R.id.dot://小数点
                inputPoint(view);
                break;
            case R.id.dis://DIS
            case R.id.multiple://"×"
            case R.id.divide://"÷"
            case R.id.plus://"+"
            case R.id.minus://"-"
                inputOperator(view);
                break;
            default://数字
                inputNumber(view);
                break;
        }
    }

    //运算结果
    private void operatorResult() {
        if (mLastInputStatus == END || mLastInputStatus == ERROR || mLastInputStatus == INPUT_OPERATOR || mInputList.size() == 1)
            return;
        findDisOperator(0);//DIS运算
        findHighOperator(0);//乘除运算
        if (mLastInputStatus != ERROR)
            findLowOperator(0);//加减运算
        showFormulaAndResult();
    }

    //显示计算公式和结果
    private void showFormulaAndResult() {
        String result = subZeroAndDot(mInputList.get(0).getInput());
        if (result.equals("-0"))
            result = "0";
        if (mLastInputStatus != ERROR) {
//            if (mResult.getText().length() > 1)
//                mResult.setText(mResult.getText() + "\n" + mInput.getText() + "=" + result);
//            else
//                mResult.setText(mInput.getText() + "=" + result);

            historySB.append(mInput.getText() + "=" + result + ";");
            showHistory();//显示历史记录
            //光标在最末尾
            listViewResult.setTranscriptMode(listViewResult.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        }
        mInput.setText(result);
        clearScreen(mInputList.get(0));
    }

    /**
     * 使用java正则表达式去掉多余的.与0
     *
     * @param s
     * @return
     */
    public static String subZeroAndDot(String s) {
        if (s.indexOf(".") > 0) {
            s = s.replaceAll("0+?$", "");// 去掉多余的0
            s = s.replaceAll("[.]$", "");// 如最后一位是.则去掉
        }
        return s;
    }

    //加减运算
    private int findLowOperator(int index) {
        if (mInputList.size() > 1 && index >= 0) {
            for (int i = index; i < mInputList.size(); i++) {
                InputItem item = mInputList.get(i);
                if (buttons[12].getText().toString().equals(item.getInput()) || buttons[13].getText().toString().equals(item.getInput())) {
                    if (mInputList.get(i - 1).getInput().equals(INPUT_ERROR) || mInputList.get(i + 1).getInput().equals(INPUT_ERROR)) {
                        equationIsError();//计算公式有误
                        return -1;
                    }
                    String a, b;
                    a = mInputList.get(i - 1).getInput();
                    b = mInputList.get(i + 1).getInput();
                    if (buttons[13].getText().toString().equals(item.getInput()))
                        mInputList.set(i - 1, new InputItem(add(a, b), InputItem.InputType.DOUBLE_TYPE));
                    else
                        mInputList.set(i - 1, new InputItem(sub(a, b), InputItem.InputType.DOUBLE_TYPE));

//                    if (mInputList.get(i - 1).getType() == InputItem.InputType.INT_TYPE) {
//                        a = mInputList.get(i - 1).getInput();
//                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
//                            b = mInputList.get(i + 1).getInput();
//                            if (buttons[13].getText().toString().equals(item.getInput()))
//                                mInputList.set(i - 1, new InputItem(add(a, b), InputItem.InputType.INT_TYPE));
//                            else
//                                mInputList.set(i - 1, new InputItem(sub(a, b), InputItem.InputType.INT_TYPE));
//                        } else {
//                            d = mInputList.get(i + 1).getInput();
//                            if (buttons[13].getText().toString().equals(item.getInput()))
//                                mInputList.set(i - 1, new InputItem(add(a, d), InputItem.InputType.DOUBLE_TYPE));
//                            else
//                                mInputList.set(i - 1, new InputItem(sub(a, d), InputItem.InputType.DOUBLE_TYPE));
//                        }
//                    } else {
//                        c = mInputList.get(i - 1).getInput();
//                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
//                            b = mInputList.get(i + 1).getInput();
//                            if (buttons[13].getText().toString().equals(item.getInput()))
//                                mInputList.set(i - 1, new InputItem(add(c, b), InputItem.InputType.DOUBLE_TYPE));
//                            else
//                                mInputList.set(i - 1, new InputItem(sub(c, b), InputItem.InputType.DOUBLE_TYPE));
//                        } else {
//                            d = mInputList.get(i + 1).getInput();
//                            if (buttons[13].getText().toString().equals(item.getInput()))
//                                mInputList.set(i - 1, new InputItem(add(c, d), InputItem.InputType.DOUBLE_TYPE));
//                            else
//                                mInputList.set(i - 1, new InputItem(sub(c, d), InputItem.InputType.DOUBLE_TYPE));
//                        }
//                    }
                    mInputList.remove(i + 1);
                    mInputList.remove(i);
                    return findLowOperator(i);
                }
            }
        }
        return -1;
    }

    //乘除运算
    private int findHighOperator(int index) {
        if (mInputList.size() > 1 && index >= 0) {
            for (int i = index; i < mInputList.size(); i++) {
                InputItem item = mInputList.get(i);
                if (buttons[10].getText().toString().equals(item.getInput()) || buttons[11].getText().toString().equals(item.getInput())) {
                    String a, b, c, d;
                    if (mInputList.get(i - 1).getType() == InputItem.InputType.INT_TYPE) {
                        a = mInputList.get(i - 1).getInput();
                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
                            b = mInputList.get(i + 1).getInput();
                            if (buttons[11].getText().toString().equals(item.getInput())) {
                                mInputList.set(i - 1, new InputItem(mul(a, b), InputItem.InputType.INT_TYPE));
                            } else {
                                if (b.equals("0")) {//b=="0"进不来这里
                                    equationIsError();//计算公式有误
                                    return -1;
                                } else if (Double.parseDouble(a) % Double.parseDouble(b) != 0)
                                    mInputList.set(i - 1, new InputItem(Double.parseDouble(a) / Double.parseDouble(b) + "", InputItem.InputType.DOUBLE_TYPE));
                                else
//                                    mInputList.set(i - 1, new InputItem(div(a, b), InputItem.InputType.INT_TYPE));
                                    mInputList.set(i - 1, new InputItem(Double.parseDouble(a) / Double.parseDouble(b) + "", InputItem.InputType.INT_TYPE));
                            }
                        } else {
                            d = mInputList.get(i + 1).getInput();
                            if (buttons[11].getText().toString().equals(item.getInput())) {
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(a) * Double.parseDouble(d) + "", InputItem.InputType.DOUBLE_TYPE));
                            } else {
                                if (d.equals("0")) {
                                    equationIsError();
                                    return -1;
                                }
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(a) / Double.parseDouble(d) + "", InputItem.InputType.DOUBLE_TYPE));
                            }
                        }
                    } else {
                        c = mInputList.get(i - 1).getInput();
                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
                            b = mInputList.get(i + 1).getInput();
                            if (buttons[11].getText().toString().equals(item.getInput())) {
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(c) * Double.parseDouble(b) + "", InputItem.InputType.DOUBLE_TYPE));
                            } else {
                                if (b.equals("0")) {
                                    equationIsError();
                                    return -1;
                                }
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(c) / Double.parseDouble(b) + "", InputItem.InputType.DOUBLE_TYPE));
                            }
                        } else {
                            d = mInputList.get(i + 1).getInput();
                            if (buttons[11].getText().toString().equals(item.getInput())) {
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(c) * Double.parseDouble(d) + "", InputItem.InputType.DOUBLE_TYPE));
                            } else {
                                if (d.equals("0")) {
                                    equationIsError();
                                    return -1;
                                }
                                mInputList.set(i - 1, new InputItem(Double.parseDouble(c) / Double.parseDouble(d) + "", InputItem.InputType.DOUBLE_TYPE));
                            }
                        }
                    }
                    mInputList.remove(i + 1);
                    mInputList.remove(i);
                    return findHighOperator(i);
                }
            }
        }
        return -1;
    }

    //DIS运算
    private int findDisOperator(int index) {
        if (mInputList.size() > 1 && index >= 0) {
            for (int i = index; i < mInputList.size(); i++) {
                InputItem item = mInputList.get(i);
                if (buttons[14].getText().toString().equals(item.getInput())) {//运算符号是DIS
//                    String a, b, c, d;
//                    a = mInputList.get(i - 1).getInput();
//                    b = mInputList.get(i + 1).getInput();
                    mInputList.set(i - 1, new InputItem(dis(mInputList.get(i - 1).getInput(), mInputList.get(i + 1).getInput()), InputItem.InputType.INT_TYPE));
//                    if (mInputList.get(i - 1).getType() == InputItem.InputType.INT_TYPE) {
//                        a = mInputList.get(i - 1).getInput();
//                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
//                            b = mInputList.get(i + 1).getInput();
//                            mInputList.set(i - 1, new InputItem(dis(a, b), InputItem.InputType.INT_TYPE));
//                        } else {
//                            d = mInputList.get(i + 1).getInput();
//                            mInputList.set(i - 1, new InputItem(dis(a, d), InputItem.InputType.INT_TYPE));
//                        }
//                    } else {
//                        c = mInputList.get(i - 1).getInput();
//                        if (mInputList.get(i + 1).getType() == InputItem.InputType.INT_TYPE) {
//                            b = mInputList.get(i + 1).getInput();
//                            mInputList.set(i - 1, new InputItem(dis(c, b), InputItem.InputType.INT_TYPE));
//                        } else {
//                            d = mInputList.get(i + 1).getInput();
//                            mInputList.set(i - 1, new InputItem(dis(c, d), InputItem.InputType.INT_TYPE));
//                        }
//                    }
                    mInputList.remove(i + 1);//删除的顺序不能错
                    mInputList.remove(i);
                    return findDisOperator(i);
                }
            }
        }
        return -1;
    }

    //计算公式有误
    private void equationIsError() {
        mLastInputStatus = ERROR;
        clearScreen(new InputItem(INPUT_ERROR, InputItem.InputType.ERROR));
    }

    //DIS算法，BigDecimal实现精确加减乘除运算
    private String dis(String str1, String str2) {
        BigDecimal b1 = new BigDecimal(str2);
        String b2 = new BigDecimal("1").subtract(b1.divide(new BigDecimal("100"))).toString();
        return mul(str1, b2);
    }

    //实现精确乘法运算
    private String mul(String str1, String str2) {
        BigDecimal b1 = new BigDecimal(str1);
        BigDecimal b2 = new BigDecimal(str2);
        return b1.multiply(b2).toString();
    }

//    //实现精确除法运算
//    private String div(String str1, String str2) {
//        BigDecimal b1 = new BigDecimal(str1);
//        BigDecimal b2 = new BigDecimal(str2);
//        return b1.divide(b2, 10, BigDecimal.ROUND_HALF_UP).toString();
//    }

    //实现精确加法运算
    private String add(String str1, String str2) {
        BigDecimal b1 = new BigDecimal(str1);
        BigDecimal b2 = new BigDecimal(str2);
        return b1.add(b2).toString();
    }

    //实现精确減法运算
    private String sub(String str1, String str2) {
        BigDecimal b1 = new BigDecimal(str1);
        BigDecimal b2 = new BigDecimal(str2);
        return b1.subtract(b2).toString();
    }

    //输入小数点
    private void inputPoint(View view) {
        if (mLastInputStatus == INPUT_POINT) return;
        if (mLastInputStatus == END || mLastInputStatus == ERROR)
            clearInputScreen();
        String key = map.get(view);
        String input = mInput.getText().toString();
        if (mLastInputStatus == INPUT_OPERATOR)
            input = input + "0";
        mInput.setText(input + key);
        addInputList(INPUT_POINT, key);
    }

    //输入运算符
    private void inputOperator(View view) {
        if (mLastInputStatus == INPUT_OPERATOR) return;
        if (mLastInputStatus == ERROR) clearInputScreen();
        if (mLastInputStatus == END)
            mLastInputStatus = INPUT_NUMBER;
        String key = map.get(view);
        if ("0".equals(mInput.getText().toString())) {
            mInput.setText("0" + key);
            mInputList.set(0, new InputItem("0", InputItem.InputType.INT_TYPE));
        } else
            mInput.setText(mInput.getText() + key);
        addInputList(INPUT_OPERATOR, key);
    }

    //输入数字
    private void inputNumber(View view) {
        if (mLastInputStatus == END || mLastInputStatus == ERROR)
            clearInputScreen();
        //限制输入数字的位数
        if (mInputList.get(mInputList.size() - 1).getInput().length() >= 20)
            return;
        String key = map.get(view);
        if ("0".equals(mInput.getText().toString()))
            mInput.setText(key);
        else
            mInput.setText(mInput.getText() + key);
        addInputList(INPUT_NUMBER, key);//添加数字
    }

    //添加数字、运算符、点
    private void addInputList(int currentStatus, String inputChar) {
        mInput.setSelection(mInput.getText().length());//焦点(光标)在添加内容的后面
        switch (currentStatus) {
            case INPUT_OPERATOR://运算符
                InputItem item = new InputItem(inputChar, InputItem.InputType.OPERATOR_TYPE);
                mInputList.add(item);
                mLastInputStatus = INPUT_OPERATOR;
                break;
            case INPUT_POINT://点
                if (mLastInputStatus == INPUT_OPERATOR) {
                    InputItem item1 = new InputItem("0" + inputChar, InputItem.InputType.DOUBLE_TYPE);
                    mInputList.add(item1);
                } else {
                    InputItem item1 = mInputList.get(mInputList.size() - 1);
                    item1.setInput(item1.getInput() + inputChar);
                    item1.setType(InputItem.InputType.DOUBLE_TYPE);
                }
                mLastInputStatus = INPUT_POINT;
                break;
            case INPUT_NUMBER://数字
                if (mLastInputStatus == INPUT_OPERATOR) {
                    InputItem item1 = new InputItem(inputChar, InputItem.InputType.INT_TYPE);
                    mInputList.add(item1);
                    mLastInputStatus = INPUT_NUMBER;
                } else if (mLastInputStatus == INPUT_POINT) {
                    InputItem item1 = mInputList.get(mInputList.size() - 1);
                    item1.setInput(item1.getInput() + inputChar);
                    item1.setType(InputItem.InputType.DOUBLE_TYPE);
                    mLastInputStatus = INPUT_POINT;
                } else if (mLastInputStatus == INPUT_NUMBER) {
                    InputItem item1 = mInputList.get(mInputList.size() - 1);
                    item1.setInput(item1.getInput() + inputChar);
                    item1.setType(InputItem.InputType.INT_TYPE);
                    mLastInputStatus = INPUT_NUMBER;
                }
                break;
        }
    }

    //回退
    private void back() {
        if (mLastInputStatus == ERROR)
            clearInputScreen();
        String str = mInput.getText().toString();
        if (str.length() != 1) {
            if (str.substring(str.length() - 1, str.length()).equals("s")) // 最后一个字符是否是“Dis”中的“s”
                mInput.setText(str.substring(0, str.length() - 3));
            else
                mInput.setText(str.substring(0, str.length() - 1));
            backList();
        } else {
            mInput.setText("0");
            clearScreen(new InputItem("", InputItem.InputType.INT_TYPE));
        }
        mInput.setSelection(mInput.getText().length());//焦点（光标）在内容的后面
    }

    //回退操作
    private void backList() {
        InputItem item = mInputList.get(mInputList.size() - 1);
        if (item.getType() == InputItem.InputType.INT_TYPE) {
            //获取最后一个item，并去掉最后一个字符
            String input = item.getInput().substring(0, item.getInput().length() - 1);
            //item为空，移除item，将操作改为运算符
            if (input.equals("")) {
                mInputList.remove(item);
                mLastInputStatus = INPUT_OPERATOR;
            } else {//否则item为未截取完的字符串，当前状态为number
                item.setInput(input);
                mLastInputStatus = INPUT_NUMBER;
            }
        } else if (item.getType() == InputItem.InputType.OPERATOR_TYPE) {// 如果item是运算操作符,则移除
            mInputList.remove(item);
            if (mInputList.get(mInputList.size() - 1).getType() == InputItem.InputType.INT_TYPE)
                mLastInputStatus = INPUT_NUMBER;
            else
                mLastInputStatus = INPUT_POINT;
        } else {//如果当前item是小数
            String input = item.getInput().substring(0, item.getInput().length() - 1);
            if (input.equals("")) {
                mInputList.remove(item);
                mLastInputStatus = INPUT_OPERATOR;
            } else {
                item.setInput(input);
                if (input.contains("."))
                    mLastInputStatus = INPUT_POINT;
                else
                    mLastInputStatus = INPUT_NUMBER;
            }
        }
    }

    //计算完成
    private void clearScreen(InputItem item) {
        if (mLastInputStatus != ERROR)
            mLastInputStatus = END;
        mInputList.clear();
        mInputList.add(item);
    }

    //清除输入屏
    private void clearInputScreen() {
        mInput.setText("0");
        mLastInputStatus = INPUT_NUMBER;
        mInputList.clear();
        mInputList.add(new InputItem("", InputItem.InputType.INT_TYPE));
    }

    //为了得到用户区域的高度，重写onWindowFocusChanged,这个方法在onResume之后被调用
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Dimension dimension = getAreaThree(this);
            screen_width = dimension.mWidth;
            screen_height = dimension.mHeight;
            //显示屏高度
            ViewGroup.LayoutParams lp = display.getLayoutParams();
            lp.height = screen_height / 3;

            int btn_width = screen_width / 4;
            int btn_height = (screen_height - screen_height / 3) / 5;//tablelayout为屏幕的2/3大，一共5行
            for (int i = 0; i < 19; i++) {
                buttons[i].setWidth(btn_width);
                buttons[i].setHeight(btn_height);
            }
            buttons[16].setHeight(btn_height * 2);
        }
    }

    private class Dimension {
        public int mWidth;
        public int mHeight;

        public Dimension() {
        }
    }

    //不算状态栏，标题栏的高度
    private Dimension getAreaThree(Activity activity) {
        Dimension dimen = new Dimension();
        // 用户绘制区域
        Rect rect = new Rect();
        activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getDrawingRect(rect);
        dimen.mWidth = rect.width();
        dimen.mHeight = rect.height();
        return dimen;
    }

    //*************************历史记录*******************************

    //显示历史记录
    private void showHistory() {
        calHistory = historySB.toString();
        if (isFirstOpenHistory) {//第一次打开，加载本地历史记录
            isFirstOpenHistory = false;
            hisHistory = load();
            if (calHistory != null) {
                updateHistory(hisHistory + calHistory);
                historySB.setLength(0);
            } else if (!TextUtils.isEmpty(hisHistory)) {
                updateHistory(hisHistory);
            }
        } else if (calHistory != null) {
            updateHistory(calHistory);
            historySB.setLength(0);
        }
    }

    //更新历史记录
    private void updateHistory(String calHistory) {
        hisHistorySB.append(calHistory);
        hisItemArray = calHistory.split(";");
        if (hisItemArray.length > 0) {
            for (String item : hisItemArray) {
                if (!TextUtils.isEmpty(item))
                    equationList.add(item.replace(",", "\n"));
            }
            eAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        save(hisHistorySB.toString() + historySB.toString());
    }

    //保存历史记录到本地
    private void save(String s) {
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try {
            out = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(s);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //加载本地保存的历史记录
    private String load() {
        FileInputStream in = null;
        BufferedReader reader = null;
        StringBuilder contentSB = new StringBuilder();
        String fileDir = getFilesDir() + File.separator + FILENAME;
        File file = new File(fileDir);
        try {
            if (!file.exists()) return "";
            in = getApplicationContext().openFileInput(FILENAME);
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null)
                contentSB.append(line);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return contentSB.toString();
    }
}
