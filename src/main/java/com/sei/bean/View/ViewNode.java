package com.sei.bean.View;

import com.alibaba.fastjson.annotation.JSONField;
import com.sei.util.SerializeUtil;
import com.sei.util.ViewUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by vector on 16/5/11.
 */
public class ViewNode implements Serializable, Comparable{

    //TODO 其实node中的viewRelateSignature和viewSignature都可以通过其他的内容构件出来的，
    //TODO 所以不应该序列化，也可以减少传输


    //表示View的类型，例如TextView等（类名）
    private String viewTag;
    public int total_view;
    private String viewText;
    public String xpath;

    public boolean isList;

    @JSONField(serialize=false)
    private int nodeHash;
    public boolean clickable;

    private int nodeRelateHash;

    //在树种的层级
    @JSONField(serialize=false)
    private int depth;

    //view的子节点
    private List<ViewNode> children;

    //view的父节点
    @JSONField(serialize=false)
    private ViewNode parent;


    //viewNodeID表示node在树中的编号
    @JSONField(serialize=false)
    private int viewNodeID;
    private int viewNodeIDRelative;

    public int resourceID;

    private int width;
    private int height;
    private int x;
    private int y;

    public ViewNode() {
        children = new LinkedList<ViewNode>();
        viewText = null;
    }

    public String getViewText() {
        return viewText;
    }
    public void setViewText(String viewText) {
        this.viewText = viewText;
    }

    public void setResourceID(int id){this.resourceID = id;}
    public int getResourceID(){return this.resourceID;}

    public int getNodeHash() {
        return nodeHash;
    }
    public void setNodeHash(int nodeHash) {
        this.nodeHash = nodeHash;
    }

    public int getNodeRelateHash() {
        return nodeRelateHash;
    }
    public void setNodeRelateHash(int nodeRelateHash) {
        this.nodeRelateHash = nodeRelateHash;
    }

    public int getViewNodeIDRelative() {
        return viewNodeIDRelative;
    }
    public void setViewNodeIDRelative(int viewNodeIDRelative) {
        this.viewNodeIDRelative = viewNodeIDRelative;
    }

    public String getViewTag() {
        return viewTag;
    }
    public void setViewTag(String viewTag) {
        this.viewTag = viewTag;
    }

    public int getDepth() {
        return depth;
    }
    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<ViewNode> getChildren() {
        return children;
    }
    public void setChildren(List<ViewNode> children) {
        this.children = children;
    }

    public ViewNode getParent() {
        return parent;
    }
    public void setParent(ViewNode parent) {
        this.parent = parent;
    }

    public int getViewNodeID() {
        return viewNodeID;
    }
    public void setViewNodeID(int viewNodeID) {
        this.viewNodeID = viewNodeID;
    }

    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }

    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }

    //返回的是class+深度+位置
    public String calString(){
        return SerializeUtil.getAbbr(this.viewTag) + "-" + depth + "-" + viewText + "-" + this.x + "-" + this.y;
    }


    public String calStringWithoutPosition(){
        return  SerializeUtil.getAbbr(this.viewTag) + "-" + this.depth;
    }

    public ViewNode findRootNode(){
        ViewNode root = parent;
        while(root != null){
            if(root.parent != null)
                root = root.parent;
            else
                break;
        }
        return root;
    }

    @Override
    public int compareTo(Object another) {
        int res = getNodeRelateHash() - ((ViewNode) another).getNodeRelateHash();
        if (res != 0)
            return res;
        res = getY() - ((ViewNode) another).getY();
        if (res != 0)
            return res;
        return getX() - ((ViewNode) another).getX();
    }

    public String getPath(){
        ViewNode vn = this;
        List<String> list = new ArrayList<>();
        while (vn != null){
            if (vn.viewNodeIDRelative == 0)
                list.add(ViewUtil.getLast(vn.getViewTag()));
            else
                break;
            vn = vn.getParent();
        }
        String res = "";
        int len = list.size();
        if (len > 0){
            res = list.get(len-1);
            if (len > 1){
                for (int i = len-2; i >= 0; --i) {
                    res += ("/" + list.get(i));
                }
            }
        }
        if (vn == null)
            return res;
        else if (res == "")
            return ""+vn.getViewNodeIDRelative();
        return vn.getViewNodeIDRelative() + "#" + res;
    }

    public String getXpath(){
        if (this.xpath == null) {
            this.xpath = ViewUtil.generate_xpath(this);
        }
        return this.xpath;
    }

    private float alpha;
    //    private Drawable background;
//    private CharSequence contentDescription;
    private boolean filterTouchesWhenObscured;
    private int id;
    private boolean keepScreenOn;
    private int layerType;
    private int left;
    private int measuredHeight;
    private int measuredWidth;
    private int nextFocusDown;
    private int nextFocusForward;
    private int nextFocusLeft;
    private int nextFocusRight;
    private int nextFocusUp;
//    private int paddingBottom;
//    private int paddingLeft;
//    private int paddingRight;
//    private int paddingTop;
    private float rotation;
    private float rotationX;
    private float rotationY;
    private int right;
    //    private Resources resources;
    private float scaleX;
    private float scaleY;
    private float scrollX;
    private float scrollY;
    private int scrollbarStyle;
    private int solidcolor;
    private float translationX;
    private float translationY;
    private int visibility;

    public boolean isFilterTouchesWhenObscured() {
        return filterTouchesWhenObscured;
    }
    public boolean isKeepScreenOn() {
        return keepScreenOn;
    }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    public float getAlpha() { return this.alpha; }
    //    public void setBackground(Drawable background) { this.background = background; }
//    public Drawable getBackground() { return this.background; }
//    public void setContentDescription(CharSequence contentDescription) { this.contentDescription = contentDescription; }
//    public CharSequence getContentDescription() { return this.contentDescription; }
    public void setFilterTouchesWhenObscured(boolean filterTouchesWhenObscured) { this.filterTouchesWhenObscured = filterTouchesWhenObscured; }
    public boolean getFilterTouchesWhenObscured() { return this.filterTouchesWhenObscured; }
    public void setId(int id) { this.id = id; }
    public int getId() { return this.id; }
    public void setKeepScreenOn(boolean keepScreenOn) { this.keepScreenOn = keepScreenOn; }
    public boolean getKeepScreenOn() { return this.keepScreenOn; }
    public void setLayerType(int layerType) { this.layerType = layerType; }
    public int getLayerType() { return this.layerType; }
    public void setLeft(int left) { this.left = left; }
    public int getLeft() { return this.left; }
    public void setMeasuredHeight(int measuredHeight) { this.measuredHeight = measuredHeight; }
    public int getMeasuredHeight() { return this.measuredHeight; }
    public void setMeasuredWidth(int measuredWidth) { this.measuredWidth = measuredWidth; }
    public int getMeasuredWidth() { return this.measuredWidth; }
    public void setNextFocusDown(int nextFocusDown) { this.nextFocusDown = nextFocusDown; }
    public int getNextFocusDown() { return this.nextFocusDown; }
    public void setNextFocusForward(int nextFocusForward) { this.nextFocusForward = nextFocusForward; }
    public int getNextFocusForward() { return this.nextFocusForward; }
    public void setNextFocusLeft(int nextFocusLeft) { this.nextFocusLeft = nextFocusLeft; }
    public int getNextFocusLeft() { return this.nextFocusLeft; }
    public void setNextFocusRight(int nextFocusRight) { this.nextFocusRight = nextFocusRight; }
    public int getNextFocusRight() { return this.nextFocusRight; }
    public void setNextFocusUp(int nextFocusUp) { this.nextFocusUp = nextFocusUp; }
    public int getNextFocusUp() { return this.nextFocusUp; }
//    public void setPaddingBottom(int paddingBottom) { this.paddingBottom = paddingBottom; }
//    public int getPaddingBottom() { return this.paddingBottom; }
//    public void setPaddingLeft(int paddingLeft) { this.paddingLeft = paddingLeft; }
//    public int getPaddingLeft() { return this.paddingLeft; }
//    public void setPaddingRight(int paddingRight) { this.paddingRight = paddingRight; }
//    public int getPaddingRight() { return this.paddingRight; }
//    public void setPaddingTop(int paddingTop) { this.paddingTop = paddingTop; }
//    public int getPaddingTop() { return this.paddingTop; }
    public void setRotation(float rotation) { this.rotation = rotation; }
    public float getRotation() { return this.rotation; }
    public void setRotationX(float rotationX) { this.rotationX = rotationX; }
    public float getRotationX() { return this.rotationX; }
    public void setRotationY(float rotationY) { this.rotationY = rotationY; }
    public float getRotationY() { return this.rotationY; }
    public void setRight(int right) { this.right = right; }
    public int getRight() { return this.right; }
    public void setScaleX(float scaleX) { this.scaleX = scaleX; }
    public float getScaleX() { return this.scaleX; }
    public void setScaleY(float scaleY) { this.scaleY = scaleY; }
    public float getScaleY() { return this.scaleY; }
    public void setScrollX(float scrollX) { this.scrollX = scrollX; }
    public float getScrollX() { return this.scrollX; }
    public void setScrollY(float scrollY) { this.scrollY = scrollY; }
    public float getScrollY() { return this.scrollY; }
    public void setScrollbarStyle(int scrollbarStyle) { this.scrollbarStyle = scrollbarStyle; }
    public int getScrollbarStyle() { return this.scrollbarStyle; }
    public void setSolidcolor(int solidcolor) { this.solidcolor = solidcolor; }
    public int getSolidcolor() { return this.solidcolor; }
    public void setTranslationX(float translationX) { this.translationX = translationX; }
    public float getTranslationX() { return this.translationX; }
    public void setTranslationY(float translationY) { this.translationY = translationY; }
    public float getTranslationY() { return this.translationY; }
    public void setVisibility(int visibility) { this.visibility = visibility; }
    public int getVisibility() { return this.visibility; }
}