package com.banlap.vcomsupportkt.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.Scroller
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 *@author Banlap on 2021/7/7
 */
class SlideRecyclerView : RecyclerView {

    private var mMoveView: ViewGroup ?= null          /**滑动的itemView*/
    private var mLastView: ViewGroup ?= null          /**末次滑动的itemView*/
    private var mFirstX: Int? = 0                     /**触碰时的首个横坐标*/
    private var mFirstY: Int? = 0                     /**触碰时的首个纵坐标*/
    private var mLastX: Int? = 0                      /**触碰末次的横坐标*/
    private var mMenuWidth: Int? = 0                  /**itemView中菜单控件宽度*/
    private var mTouchSlop: Int = 0                   /**最小滑动距离*/
    private var mScroller: Scroller ?= null
    private var mVelocity: VelocityTracker ?= null    /** 速度跟踪 */
    private var mMoving: Boolean = false              /**是否正在水平滑动*/
    companion object {
        private const val MINIMUM_VELOCITY = 500      /**最小速度*/
    }

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) { init() }

    private fun init() {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mScroller = Scroller(context)
    }

    /** 拦截触摸事件*/
    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        val x = e?.x?.toInt() ?:0
        val y = e?.y?.toInt() ?:0
        addVelocityEvent(e)
        when(e?.action) {
            MotionEvent.ACTION_DOWN -> {
                //若Scroller处于动画中，则终止动画
                if(!mScroller!!.isFinished) {
                    mScroller!!.abortAnimation()
                }
                mFirstX = x
                mFirstY = y
                mLastX = x
                val xToF = x.toFloat()
                val yToF = y.toFloat()
                //获取点击区域所在的itemView
                mMoveView = findChildViewUnder(xToF, yToF) as ViewGroup?
                //在点击区域以外的itemView开着菜单，则关闭菜单
                if(mLastView !=null && mLastView != mMoveView && mLastView?.scrollX !=0) {
                    closeMenu()
                }
                //获取itemView中菜单的宽度（规定itemView中为两个子View）
                mMenuWidth =
                    if(mMoveView !=null && mMoveView?.childCount ==2 ) {
                        mMoveView!!.getChildAt(1).width
                    } else {
                        -1
                    }
            }
            MotionEvent.ACTION_MOVE -> {
                mVelocity!!.computeCurrentVelocity(1000)
                val velocityX = abs(mVelocity!!.xVelocity).toInt()
                val velocityY = abs(mVelocity!!.yVelocity).toInt()
                val moveX = abs(x - mFirstX!!)
                val moveY = abs(y - mFirstY!!)
                //满足如下条件其一则判定为水平滑动：
                //1、水平速度大于竖直速度,且水平速度大于最小速度
                //2、水平位移大于竖直位移,且大于最小移动距离
                //必需条件：itemView菜单栏宽度大于0，且recyclerView处于静止状态（即并不在竖直滑动和拖拽）
                val isHorizontalMove = ( abs(velocityX) >= MINIMUM_VELOCITY && velocityX > velocityY || moveX > moveY
                        && moveX > mTouchSlop) && mMenuWidth!! > 0 && scrollState == 0
                if(isHorizontalMove) {
                    mMoving = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                releaseVelocity()
                //itemView以及其子view触发触碰事件(点击、长按等)，菜单未关闭则直接关闭
                closeMenuNow()
            }

        }
        return super.onInterceptTouchEvent(e)
    }

    /** 触摸事件 */
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        val x = e?.x?.toInt() ?:0
        val y = e?.y?.toInt() ?:0
        addVelocityEvent(e)
        when(e?.action) {
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> {
                if(mMoving) {
                    val dx = mLastX!! - x
                    if(mMoveView!!.scrollX + dx >=0 && mMoveView!!.scrollX + dx <= mMenuWidth!!) {
                        mMoveView!!.scrollBy(dx, 0)
                    }
                    mLastX = x
                    return true
                } else {
                    mVelocity!!.computeCurrentVelocity(1000)
                    val velocityX = abs(mVelocity!!.xVelocity).toInt()
                    val velocityY = abs(mVelocity!!.yVelocity).toInt()
                    val moveX = abs(x - mFirstX!!)
                    val moveY = abs(y - mFirstY!!)
                    //根据水平滑动条件判断，是否让itemView跟随手指滑动
                    //这里重新判断是避免itemView中不拦截ACTION_DOWN事件，则后续ACTION_MOVE并不会走onInterceptTouchEvent()方法
                    val isHorizontalMove = ( abs(velocityX) >= MINIMUM_VELOCITY && velocityX > velocityY || moveX > moveY
                            && moveX > mTouchSlop) && mMenuWidth!! > 0 && scrollState == 0
                    if(isHorizontalMove) {
                        val dx = mLastX!! - x
                        //让itemView在规定区域随手指移动
                        if(mMoveView!!.scrollX + dx >=0 && mMoveView!!.scrollX + dx <= mMenuWidth!!) {
                            mMoveView!!.scrollBy(dx, 0)
                        }
                        mLastX = x
                        //设置正处于水平滑动状态
                        mMoving = true
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mMoving) {
                    //先前没结束的动画终止，并直接到终点
                    if(!mScroller!!.isFinished) {
                        mScroller!!.abortAnimation()
                        mLastView!!.scrollTo(mScroller!!.finalX, 0)
                    }
                    mMoving = false
                    //已放手，即现滑动的itemView成了末次滑动的itemView
                    mLastView = mMoveView
                    mVelocity!!.computeCurrentVelocity(1000)
                    val scrollX = mLastView!!.scrollX
                    //若速度大于正方向最小速度，则关闭菜单栏；若速度小于反方向最小速度，则打开菜单栏
                    //若速度没到判断条件，则对菜单显示的宽度进行判断打开/关闭菜单
                    when {
                        mVelocity!!.xVelocity >= MINIMUM_VELOCITY -> {
                            mScroller!!.startScroll(scrollX, 0, -scrollX, 0, abs(scrollX))
                        }
                        mVelocity!!.xVelocity <= -MINIMUM_VELOCITY -> {
                            val dx = mMenuWidth!! - scrollX
                            mScroller!!.startScroll(scrollX, 0, dx, 0, abs(dx))
                        }
                        scrollX > mMenuWidth!!/2 -> {
                            val dx = mMenuWidth!! - scrollX
                            mScroller!!.startScroll(scrollX, 0, dx, 0, abs(dx))
                        }
                        else -> {
                            mScroller!!.startScroll(scrollX, 0, -scrollX, 0, abs(scrollX))
                        }
                    }

                } else if (mLastView !=null && mLastView!!.scrollX !=0) {
                    //若不是水平滑动状态，菜单栏开着则关闭
                    closeMenu()
                }
                releaseVelocity()
            }
        }
        return super.onTouchEvent(e)
    }

    override fun computeScroll() {
        if(mScroller!!.computeScrollOffset()) {
            if(isInWindow(mLastView!!)) {
                mLastView!!.scrollTo(mScroller!!.currX, 0)
                invalidate()
            } else {
                //若处于动画的itemView滑出屏幕，则终止动画，并让其到达结束点位置
                mScroller!!.abortAnimation()
                mLastView!!.scrollTo(mScroller!!.finalX, 0)
            }
        }
    }

    /**
     * 使用Scroller关闭菜单栏
     */
    private fun closeMenu() {
        mScroller!!.startScroll(mLastView!!.scrollX, 0, -mLastView!!.scrollX, 0, 500)
        invalidate()
    }

    /**
     * 获取VelocityTracker实例，并为其添加事件
     * @param e 触碰事件
     */
    @SuppressLint("Recycle")
    private fun addVelocityEvent(e: MotionEvent?) {
        mVelocity = mVelocity?: VelocityTracker.obtain()
        mVelocity!!.addMovement(e)
    }

    /**
     * 释放VelocityTracker
     */
    private fun releaseVelocity() {
        if(mVelocity !=null) {
            mVelocity!!.clear()
            mVelocity!!.recycle()
            mVelocity = null
        }
    }

    /**
     * 即刻关闭菜单栏
     */
    private fun closeMenuNow() {
        if(mLastView !=null && mLastView!!.scrollX !=0) {
            mLastView!!.scrollTo(0, 0)
        }
    }

    /**
     * 判断该itemView是否显示在屏幕内
     * @param view itemView
     * @return isInWindow
     */
    private fun isInWindow(view: View): Boolean {
        if(layoutManager is LinearLayoutManager){
            val linearLayoutManager: LinearLayoutManager = layoutManager as LinearLayoutManager
            val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
            val lastPosition = linearLayoutManager.findLastVisibleItemPosition()
            val currentPosition = linearLayoutManager.getPosition(view)
            return currentPosition in firstPosition..lastPosition
        }
        return true
    }

}