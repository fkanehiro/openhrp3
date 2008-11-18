#ifndef __GUTIL_H__
#define __GUTIL_H__

namespace geometry 
{

template<typename Predicate, typename Iterator>
bool some_true(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (predicate(*p)) { return true; }
  }
  return false;
}

template<typename Predicate, typename Iterator>
bool some_false(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (!predicate(*p)) { return true; }
  }
  return false;
}

template<typename Predicate, typename Iterator>
bool all_false(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (predicate(*p)) { return false; }
  }
  return true;
}

template<typename Predicate, typename Iterator>
bool all_true(Predicate predicate, Iterator begin, Iterator end)
{
  for (Iterator p = begin; p < end; p++) {
    if (!predicate(*p)) { return false; }
  }
  return true;
}


enum CompareMode {Lenient, Strict};

template<typename T>
bool ascending(const T& a, const T& b, const T& c, CompareMode mode)
{
  return (mode == Strict) ? ((a < b) && (b < c)) : ((a <= b) && (b <= c));
}

template<typename T>
bool descending(const T& a, const T& b, const T& c, CompareMode mode)
{
  return ascending(c, b, a, mode);
}


template<typename T>
int sign(const T& t)
{
  if (t < (T)0) { return -1; }
  if (t > (T)0) { return  1; }
  return 0;
}


}//namespace geometry

#endif //__GUTIL_H__

