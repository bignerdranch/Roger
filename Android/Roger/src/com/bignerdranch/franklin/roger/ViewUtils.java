package com.bignerdranch.franklin.roger;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A set of handy static tools to use on Views.
 */
public abstract class ViewUtils {
    /**
     * Just a boolean function.
     */
    public interface Criteria {
        /**
         * Specifies whether you really want this view or not.
         * @param view A view to check out.
         * @return True to accept this view.
         */
        public boolean matches(View view);
    }

    /**
     * List all the view children of this object in parent-first order.
     * @param view A view, or an Activity.
     * @return A flattened list of all the views rooted at that object.
     */
    public static ArrayList<View> listHierarchy(Object view) {
        return findViewsByCriteria(view, new Criteria() {
            public boolean matches(View view) { return true; }
        });
    }

    /**
     * Freeze all views in a hierarchy.
     * @param view A view, or an Activity.
     */
    public static void freezeViews(Object view) {
        freezeViewsWithCriteria(view, new Criteria() {
            public boolean matches(View view) { return true; }
        });
    }

    /**
     * Freeze all views in a hierarchy that match a given Criteria.
     * @param view A view, or an Activity.
     * @param criteria Criteria for views to freeze.
     */
    public static void freezeViewsWithCriteria(Object view, Criteria criteria) {
        for (View child : findViewsByCriteria(view, criteria)) {
            freeze(child);
        }
    }

    static int[] location = new int[2];
    public static boolean intersect(View v, float rawX, float rawY) {
        v.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        if (rawX < left || rawY < top) {
            return false;
        } else if (rawX > left + v.getWidth()) {
            return false;
        } else if (rawY > top + v.getHeight()) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean intersect(View a, View b) {
        Rect rectA = new Rect();
        Rect rectB = new Rect();
        a.getGlobalVisibleRect(rectA);
        b.getGlobalVisibleRect(rectB);

        return rectA.intersect(rectB);
    }

    public static void reparent(View view, RelativeLayout newParent) {
        freeze(view);
        if (!(view.getParent() instanceof ViewGroup)) {
            // nothing we can do here...
            return;
        }
        ViewGroup oldParent = (ViewGroup)view.getParent();
        Point distance = getDistance(newParent, view);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(view.getWidth(), view.getHeight());
        params.leftMargin = distance.x;
        params.topMargin = distance.y;
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        oldParent.removeView(view);
        newParent.addView(view, params);
    }

    public static Point getDistanceToAncestor(View a, ViewGroup targetAncestor) {
        Point distance = new Point();
        distance.x = a.getLeft();
        distance.y = a.getTop();
        for (ViewGroup ancestor : getAncestors(a)) {
            if (ancestor == targetAncestor) {
                return distance;
            } else {
                distance.offset(ancestor.getLeft(), ancestor.getTop());
            }
        }

        return null;
    }

    public static Point getDistance(View a, View b) {
        ViewGroup parent = getFirstCommonAncestor(a, b);

        // can't very well have a good answer here
        if (parent == null) return null;

        Point distanceA = getDistanceToAncestor(a, parent);
        Point distanceB = getDistanceToAncestor(b, parent);

        return new Point(distanceB.x - distanceA.x, 
                distanceB.y - distanceA.y);
    }

    public static ArrayList<ViewGroup> getAncestors(View v) {
        ArrayList<ViewGroup> ancestors = new ArrayList<ViewGroup>();

        ViewParent parent = v.getParent();
        while (parent != null && parent instanceof ViewGroup) {
            ancestors.add((ViewGroup)parent);
            parent = parent.getParent();
        }

        return ancestors;
    }

    public static ViewGroup getFirstCommonAncestor(View a, View b) {
        HashSet<ViewGroup> ancestorsOfA = new HashSet<ViewGroup>(getAncestors(a));
        for (ViewGroup ancestorOfB : getAncestors(b)) 
            if (ancestorsOfA.contains(ancestorOfB)) 
                return ancestorOfB;

        return null;
    }

    /**
     * Freeze a view so that its layout width and height equal its current 
     * width and height, rather than being match_parent or wrap_content.
     * Pin in its current location, too, if it's a FrameLayout or RelativeLayout
     * child.
     * @param view A view to freeze.
     */
    public static void freeze(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        int left = view.getLeft();
        int top = view.getTop();

        ViewGroup.LayoutParams params = view.getLayoutParams();
        
        // if it's a RelativeLayout, pin it x/y, too
        if (params.getClass().equals(RelativeLayout.LayoutParams.class)) {
            RelativeLayout.LayoutParams relParams = new RelativeLayout.LayoutParams(width, height);

            relParams.leftMargin = left;
            relParams.topMargin = top;
            relParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            relParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            params = relParams;
        // same for FrameLayout
        } else if (params.getClass().equals(FrameLayout.LayoutParams.class)) {
            FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(width, height);

            frameParams.leftMargin = left;
            frameParams.topMargin = top;
            frameParams.gravity = Gravity.TOP & Gravity.LEFT;

            params = frameParams;
        }

        params.width = width;
        params.height = height;

        view.setLayoutParams(params);
    }

    public static void freezeTo(View view, View modelView) {
        int width = modelView.getWidth();
        int height = modelView.getHeight();

        ViewGroup.LayoutParams params = view.getLayoutParams();
        
        params.width = width;
        params.height = height;

        view.setLayoutParams(params);
    }

    /**
     * Get a breakout box for the view. The breakout box is stored in the tag for
     * this view, so don't expect to be able to use that tag for anything else.
     * See {@link BreakoutBox} for more info - usually instead of calling this
     * method a caller will call overloads of {@link setTag} and {@link getTag}.
     * @param view View to get the breakout box for.
     * @return An instance of BreakoutBox from the view's tag. If the tag is not
     *      set, or the tag is not a BreakoutBox, it is set to be a new instance
     *      of BreakoutBox.
     */
    public static BreakoutBox getBreakoutBox(View view) {
        Object tag = view.getTag();
        if (tag == null || !(tag instanceof BreakoutBox)) {
            tag = new BreakoutBox(view);
            view.setTag(tag);
        }

        return (BreakoutBox)tag;
    }

    /**
     * Set a tag by resource id in a safe manner. This method is meant to replace 
     * {@link View.setTag(int, Object)}, which will leak memory if used to store
     * a View within the same activity as the owning view.
     *
     * Calling this method will replace whatever value was in the view's tag.
     * @param view A view to set a tag value for.
     * @param resId A resource id to use as a key for that value.
     * @param o An object to store.
     */
    public static void setTag(View view, int resId, Object o) {
        getBreakoutBox(view).setTag(resId, o);
    }

    /**
     * Get a tag by resource id in a safe manner. This method is meant to replace 
     * {@link View.getTag(int)}, which will leak memory if used to store
     * a View within the same activity as the owning view.
     *
     * Calling this method without having called {@link ViewUtils.setTag} is
     * guaranteed to ruin your day.
     * @param view A view to get a tag value for.
     * @param resId A resource id to use as a key for that value.
     * @return The object stored for this view at that key.
     */
    public static Object getTag(View view, int resId) {
        return getBreakoutBox(view).getTag(resId);
    }

    /**
     * Set a tag in a safe manner. This method behaves identically to {@link View.setTag(Object)},
     * but plays well with {@link setTag(View,int,Object)}.
     * @param view A view to set a tag value for.
     * @param o An object to store.
     */
    public static void setTag(View view, Object o) {
        getBreakoutBox(view).setTag(o);
    }

    /**
     * Get a tag in a safe manner. This method behaves identically to {@link View.getTag()},
     * but plays well with {@link setTag(View,int,Object)}.
     * @param view A view to set a tag value for.
     * @param o An object to store.
     */
    public static Object getTag(View view) {
        return getBreakoutBox(view).getTag();
    }

    /**
     * Do some operation on a view while preserving its various properties that
     * tend to get nuked when you do things like set the background. The View's
     * padding will be saved, and if it is a {@link TextView} its compound
     * drawable padding will be saved as well. Then the operation is run, and
     * then the padding values are set on the view again.
     * @param view A view to save and restore values to.
     * @param operation Some code to run. Usually this is setting the background.
     */
    public static void setViewPreservedly(View view, Runnable operation) {
        int[] padding = new int[] { 
            view.getPaddingLeft(),
            view.getPaddingTop(),
            view.getPaddingRight(),
            view.getPaddingBottom() 
        };
        int compoundDrawablePadding = 0;
        if (view instanceof TextView) {
            compoundDrawablePadding = ((TextView)view).getCompoundDrawablePadding();
        }

        operation.run();

        view.setPadding(padding[0], padding[1], padding[2], padding[3]);
        if (view instanceof TextView) {
            ((TextView)view).setCompoundDrawablePadding(compoundDrawablePadding);
        }
    }

    /** 
     * Calls setBackgroundResource without nuking any of the padding
     * and other goodness we require in our views.
     * @param view A view to set the background on.
     * @param id A resource id for the background.
     */
    public static void setBackgroundResource(final View view, final int id) {
        setViewPreservedly(view, new Runnable() { public void run() {
            view.setBackgroundResource(id);
        }});
    }

    /**
     * Finds a subview of the specified view by ID, automatically tagging the view with the ID/subview combo.
     * @param view The parent view.
     * @param id The resource ID of a subview.
     * @return The subview, optimized by caching it as a tag.
     */
    public static View findTaggedViewById(View view, int id) {
        BreakoutBox box = getBreakoutBox(view);

        Object value = box.getTag(id);
        if (value != null && value instanceof View) {
            return (View)value;
        }

        View subView = view.findViewById(id);
        if (subView != null) {
            box.setTag(id, subView);
        }

        return subView;
    }

    /**
     * Finds all subviews of the given view root with an id equal to the given resource id.
     * @param view A view root.
     * @param resId A resource id to look for.
     * @return A list of all subviews for the root view (including the root view) for which 
     *      subview.getId() == resId. This list is guaranteed to be non null.
     */
    public static ArrayList<View> findViewsById(final Object view, final int resId) {
        return findViewsByCriteria(view, new Criteria() { 
            public boolean matches(View view) { 
                return resId == view.getId();
            }
        });
    }

    /**
     * Finds all subviews of the given view root where {@link getTag(View,int)} is non null and equal to
     * the given value.
     * @param view A view root.
     * @param tagId A resource id to look for as a tag key.
     * @param value An object that is expected to equal that tag value.
     * @return A list of all subviews for the root view (including the root view) for which 
     *      getTag(subview, tagId).equals(value). This list is guaranteed to be non null.
     */
    public static ArrayList<View> findViewsByTag(final Object view, final int tagId, final Object value) {
        return findViewsByCriteria(view, new Criteria() { 
            public boolean matches(View view) { 
                Object o = getTag(view, tagId);

                return o != null && o.equals(value);
            }
        });
    }

    /**
     * Finds all subviews of the given view root where {@link getTag(View,int)} is non null.
     * @param view A view root.
     * @param tagId A resource id to look for as a tag key.
     * @return A list of all subviews for the root view (including the root view) for which 
     *      getTag(subview, tagId) != null. This list is guaranteed to be non null.
     */
    public static ArrayList<View> findViewsByTag(final Object view, final int tagId) {
        return findViewsByCriteria(view, new Criteria() { 
            private int tag;
            public Criteria init(int tag) { this.tag = tag; return this;}
            public boolean matches(View view) { 
                return getTag(view, tag) != null;
            }
        }.init(tagId));
    }

    /**
     * Finds all subviews of the given view root where {@link getTag(View)} is non null
     * and equal to the given value.
     * @param view A view root.
     * @param tag A value to look for.
     * @return A list of all subviews for the root view (including the root view) for which 
     *      getTag(subview, tagId).equals(tag). This list is guaranteed to be non null.
     */
    public static ArrayList<View> findViewsByTag(final Object view, final Object tag) {
        return findViewsByCriteria(view, new Criteria() { 
            public boolean matches(View view) { 
                return getTag(view) != null && getTag(view).equals(tag);
            }
        });
    }

    /**
     * Find every view that is a parent, grandparent, etc of the given view.
     * @param view A view.
     * @return A list of all parents of that view, starting with the parent, then 
     *      grandparent, and so on.
     */
    public static ArrayList<View> getParentViews(View view) {
        ArrayList<View> views = new ArrayList<View>();
        views.add(view);

        ViewParent parent = view.getParent();

        while (parent != null && parent instanceof View) {
            View parentView = (View)parent;
            views.add(parentView);
            parent = parentView.getParent();
        }

        return views;
    }

    /**
     * Get views in a view's hierarchy that match an arbitrary criteria.
     * For each view in the view hierarchy rooted at view, {@link Criteria.matches}
     * is called and, if true, the view will be in the list returned.
     * @param view A view root to scan.
     * @param criteria A criteria the returned views should satisfy.
     * @return A list of views satisfying that criteria.
     */
    public static ArrayList<View> findViewsByCriteria(Object view, Criteria criteria) {
        ArrayList<View> found = new ArrayList<View>();

        View baseView;
        if (view == null) {
            return found;
        } else if (view instanceof View) {
            baseView = (View)view;
        } else if (view instanceof Activity) {
            baseView = ((Activity)view).getWindow().getDecorView();
        } else {
            throw new IllegalArgumentException("Unsupported argument type: " + view.getClass().getName());
        }

        findViewsByCriteria(baseView, criteria, found);
        return found;
    }

    private static void findViewsByCriteria(View view, Criteria criteria, ArrayList<View> found) {
        if (view == null) 
            return;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;

            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                findViewsByCriteria(group.getChildAt(i), criteria, found);
            }
        }

        if (criteria == null || criteria.matches(view)) {
            found.add(view);
        }
    }

    public static void onLayout(final View v, final Runnable r) {
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                r.run();
                
                v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }
    
    /**
     * Find all views of a given class in a view hierarchy.
     * @param vg A view group.
     * @param klass A class to find instances of.
     */
    public static <T> ArrayList<T> findViewsByClass(ViewGroup vg, Class<T> klass) {
        ArrayList<T> instances = new ArrayList<T>();

        fillViewsWithClass(vg, klass, instances);

        return instances;
    }

    private static <T> void fillViewsWithClass(ViewGroup vg, Class<T> klass, ArrayList<T> instances) {
        int childCount = vg.getChildCount();
        for (int i=0; i < childCount; i++) {
            View v = vg.getChildAt(i);
            if (v.getClass().equals(klass)) 
                instances.add(klass.cast(v));
            if (v instanceof ViewGroup) {
                fillViewsWithClass((ViewGroup)v, klass, instances);
            }            
        }
    }

    /**
     * Find a single view of a given class in a view hierarchy. As soon
     * as a view is find whose class .equals the given class, it is returned.
     * @param vg A view group.
     * @param klass A class to find an instance of.
     */
    public static <T> T findViewByClass(ViewGroup vg, Class<T> klass) {
        int childCount = vg.getChildCount();
        T child = null;
        for (int i=0; i < childCount; i++) {
            View v = vg.getChildAt(i);
            if (v.getClass().isInstance(klass)) 
                return klass.cast(v);
            else if (v instanceof ViewGroup) {
                child = findViewByClass((ViewGroup)v, klass);
                if (child != null)
                    return child;
            }            
        }
        return child;
    }
}

