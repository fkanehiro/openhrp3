#include <fstream>
#include <iostream>
#include "TimeUtil.h"

tick_t get_tick()
{
#ifdef _WIN32
    LARGE_INTEGER t;
    QueryPerformanceCounter(&t);
    return t.QuadPart;
#else
    unsigned int l=0,h=0;
    __asm__ __volatile__("rdtsc" : "=a" (l), "=d" (h));
    return (unsigned long long)h<<32|l;
#endif
}

double get_cpu_frequency()
{
    static double freq = -1;
    if (freq != -1) return freq;
#if defined(_WIN32)
    LARGE_INTEGER li;
    QueryPerformanceFrequency(&li);
    freq = (double)li.QuadPart;
#elif defined(__QNX__)
    freq = SYSPAGE_ENTRY( qtime )->cycles_per_sec;
#elif defined(__APPLE__)
    // how to get cpu clock on Mac?
    freq = 2.8e9;
#else
    std::ifstream ifs("/proc/cpuinfo");
    if (!ifs.is_open()){
        std::cerr << "failed to open /proc/cpuinfo" << std::endl;
        return 0;
    }
    std::string token;
    while(!ifs.eof()){
        ifs >> token;
        if (token == "cpu"){
            ifs >> token;
            if (token == "MHz"){
                ifs >> token;
                ifs >> freq;
                freq *= 1e6;
                break;
            }
        }
    }
    ifs.close();
#endif
    return freq;
}

