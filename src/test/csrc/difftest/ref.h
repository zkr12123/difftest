/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

/**
 * Headers for C/C++ REF models
 */
#ifndef __DT_REF_H__
#define __DT_REF_H__

#include "difftest.h"

// REF Models
extern "C" uint8_t pte_helper(uint64_t satp, uint64_t vpn, uint64_t *pte, uint8_t *level);

typedef union PageTableEntry {
  struct {
    uint64_t v   : 1;
    uint64_t r   : 1;
    uint64_t w   : 1;
    uint64_t x   : 1;
    uint64_t u   : 1;
    uint64_t g   : 1;
    uint64_t a   : 1;
    uint64_t d   : 1;
    uint64_t rsw : 2;
    uint64_t ppn :44;
    uint64_t pad :10;
  };
  struct {
    uint64_t difftest_v       : 1;
    uint64_t difftest_perm    : 7;
    uint64_t difftest_rsw     : 2;
    uint64_t difftest_ppn     :24;
    uint64_t difftest_ppn_high:20;
    uint64_t difftest_pad     :10;
  };
  uint64_t val;
} PTE;

#define VPNi(vpn, i) (((vpn) >> (18 - 9 * (i))) & 0x1ff)
#endif
