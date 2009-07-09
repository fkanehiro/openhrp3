#ifndef __VECTOR_H__
#define __VECTOR_H__

#include <algorithm>
#include <numeric>
#include <cmath>
#include <vector>

#include "gutil.h"


namespace geometry
{

template<typename Scalar, size_t Dim>
class Vector
{
public:
  Vector(): _dim(Dim) { std::fill(_data, _data + Dim, (Scalar)0.0); }
  Vector(Scalar  s): _dim(Dim) { std::fill(_data, _data + Dim, s); }
  Vector(Scalar* a): _dim(Dim) { std::copy(a, a + Dim, _data); }

#if 0  // intentionally omitted
  Vector(const Vector& that) {..}
  Vector& operator=(const Vector& that) {..}
#endif

  inline
  size_t dimension() const { return _dim; } 

  inline
  Scalar* data() { return _data; }

  inline
  const Scalar* data() const { return _data; }

  inline
  operator Scalar*() { return _data; }

  inline
  operator const Scalar*() const { return _data; }

  inline
  Scalar& operator[](int index) { return _data[index]; }

  inline 
  const Scalar& operator[](int index) const { return _data[index]; }

  inline
  Scalar* begin() { return _data; }
  inline
  Scalar* end()   { return _data + Dim; }

  inline
  const Scalar* begin() const { return _data; }
  inline
  const Scalar* end()   const { return _data + Dim; }

  void shift(int d)
  {
    if (d < 0) { d = Dim - (-d % Dim); }
    d %= Dim;
    if (d == 0) { return; }
    std::rotate(begin(), end() - d, end());
  }

  inline
  void negate() 
  { 
    std::transform(begin(), end(), begin(), std::negate<Scalar>()); 
  }

  inline
  void operator+=(Vector that)
  {
    std::transform(begin(), end(), that.begin(), begin(), std::plus<Scalar>());
  }

  inline
  void operator-=(Vector that)
  {
    std::transform(begin(), end(), that.begin(), begin(), std::minus<Scalar>());
  }

  inline
  void operator*=(Scalar s) 
  {
    std::transform(begin(), end(), begin(), std::bind2nd(std::multiplies<Scalar>(), s));
  }

  inline
  void operator/=(Scalar s)
  {
    std::transform(begin(), end(), begin(), std::bind2nd(std::divides<Scalar>(), s));
  }

  Scalar norm1() const
  {
    Scalar data[Dim];
    std::transform(begin(), end(), data, abs);
    return sum(data);
  }

  inline
  Scalar norm2() const { return dot(_data, _data); }

  inline
  Scalar norm() const { return std::sqrt(norm2()); }

  static Scalar AlmostZero;

  inline
  static bool is_zero(Scalar s) { return abs(s) <= AlmostZero; }

  inline
  static bool is_zero(const Vector& v) { return is_zero(v.norm1()); }

protected:
  inline
  static Scalar abs(Scalar s) { return (s >= (Scalar)0.0) ? s : -s; }

  inline
  static Scalar sum(Scalar a[Dim]) { return std::accumulate(a, a + Dim, (Scalar)0.0); }

  inline
  static Scalar dot(const Scalar a[Dim], const Scalar b[Dim]) 
  { 
    return std::inner_product(a, a + Dim, b, (Scalar)0.0);
  }

private:
  size_t _dim;
  Scalar _data[Dim];

};//class Vector


template<typename Scalar, size_t Dim>
Scalar Vector<Scalar,Dim>::AlmostZero = (Scalar)1.0e-10;


template<typename Scalar, size_t Dim> inline
bool operator==(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b) 
{
  return (&a == &b) || Vector<Scalar,Dim>::is_zero((a - b).norm1());
}

template<typename Scalar, size_t Dim> inline
bool operator!=(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b) { return !(a == b); }


template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator-(const Vector<Scalar,Dim>& v)
{
  Vector<Scalar,Dim> w = v; w.negate(); return w;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator+(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  Vector<Scalar,Dim> c = a; c += b; return c;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator-(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  Vector<Scalar,Dim> c = a; c -= b; return c;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator*(const Vector<Scalar,Dim>& v, Scalar s) 
{ 
  Vector<Scalar,Dim> w = v; w *= s; return w;
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> operator/(const Vector<Scalar,Dim>& v, Scalar s)
{
  Vector<Scalar,Dim> w = v; w /= s; return w;
}


template<typename Scalar, size_t Dim>
Scalar dot(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  return std::inner_product(a.begin(), a.end(), b.begin(), (Scalar)0.0);
}

template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> cross(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  Scalar c[Dim];
  for (int h = 0; h < Dim; ++h) {
    int i = (h + 1) % Dim;
    int j = (h + 2) % Dim;
    c[h] = (a[i] * b[j]) - (a[j] * b[i]);
  }
  return Vector<Scalar,Dim>(c);
}


#if 1
template<typename Element, typename Scalar>
Element sum(const std::vector<Element>& elements, const std::vector<Scalar>& coeffs)
{
  //if (elements.size() != coeffs.size()) { throw std::invalid_argument("..."); }
  Element s;
  int lim = elements.size();
  for (int i = 0; i < lim; ++i) {
    s += (elements[i] * coeffs[i]);
  }
  return s;
}
#else
template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> sum(const std::vector< Vector<Scalar,Dim> >& vectors, const std::vector<Scalar>& coeffs)
{
  //if (vectors.size() != coeffs.size()) { throw std::invalid_argument("..."); }
  Vector<Scalar,Dim> s;
  int lim = vectors.size();
  for (int i = 0; i < lim; ++i) {
    s += (vectors[i] * coeffs[i]);
  }
  return s;
}
#endif

#if 1
template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> cyclic_cross_sum(const std::vector< Vector<Scalar,Dim> >& vectors)
{
  typedef Vector<Scalar,Dim> V;
  V ccs;
  int size = vectors.size();
  for (int i = 0; i < size; ++i) {
    V a = vectors[i];
    V b = vectors[(i + 1) % size]; 	// taken circularly
    ccs += cross(a,b);
  }
  return ccs;
}
#else
template<typename Element, typename Result>
Result cyclic_cross_sum(const std::vector<Element>& elements)
{
  Result ccs;
  int size = elements.size();
  for (int i = 0; i < size; ++i) {
    Element a = elements[i];
    Element b = elements[(i + 1) % size]; 	// taken circularly
    ccs += cross(a,b);
  }
  return ccs;
}
#endif

#if 1
template<typename Scalar, size_t Dim>
Vector<Scalar,Dim> cyclic_cross_sum(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b, const Vector<Scalar,Dim>& c)
{
  //return (cross(a,b) + cross(b,c) + cross(c,a)); 	// definition-faithful
  return cross(b - a, c - a); 				// more efficient
}
#else
template<typename Element, typename Result>
Result cyclic_cross_sum(const Element& a, const Element& b, const Element& c)
{
  //return (cross(a,b) + cross(b,c) + cross(c,a)); 	// definition-faithful
  return cross(b - a, c - a); 				// more efficient
}
#endif


template<typename Scalar, size_t Dim> inline
bool parallel(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{
  return Vector<Scalar,Dim>::is_zero(cross(a,b));
}

template<typename Scalar, size_t Dim> inline
bool orthogonal(const Vector<Scalar,Dim>& a, const Vector<Scalar,Dim>& b)
{ 
  return Vector<Scalar,Dim>::is_zero(dot(a,b));
}


}//namespace geometry

#endif //__VECTOR_H__

