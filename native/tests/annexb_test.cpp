// SPDX-License-Identifier: GPL-2.0-or-later
#include "../annexb.h"
#include <cassert>
#include <vector>

int main() {
  // Mixed three/four-byte prefixes, BL slice, nested EL and original P7 RPU.
  // The RPU payload includes escaped start-code-like bytes, which must be
  // untouched.
  const std::vector<uint8_t> input = {
      0, 0,    0, 1,    0x26, 1,    0xaa, 0xbb, 0,    0, 1, 0x7e, 1, 0x26,
      1, 0xcc, 0, 0,    0,    1,    0x7c, 1,    0x19, 0, 0, 3,    1, 0xff,
      0, 0,    1, 0x7e, 1,    0x02, 1,    0xdd, 0,    0, 1, 0x02, 1, 0xee,
  };
  const std::vector<uint8_t> expected = {
      0, 0,    0, 1, 0x26, 1, 0xaa, 0xbb, 0, 0, 0,    1, 0x7c,
      1, 0x19, 0, 0, 3,    1, 0xff, 0,    0, 1, 0x02, 1, 0xee,
  };
  auto actual = input;
  actual.resize(wholphin::stripEl(actual.data(), actual.size()));
  assert(actual ==
         expected); // BL and original RPU remain byte-for-byte identical.
  assert(wholphin::stripEl(actual.data(), actual.size()) == actual.size());
  // Respect the decoder buffer offset and don't touch the surrounding guard
  // bytes.
  std::vector<uint8_t> guarded(4, 0xa5);
  guarded.insert(guarded.end(), input.begin(), input.end());
  guarded.insert(guarded.end(), 4, 0xa5);
  auto size = wholphin::stripEl(guarded.data() + 4, input.size());
  assert(std::vector<uint8_t>(guarded.begin() + 4,
                              guarded.begin() + 4 + size) == expected);
  for (int i = 0; i < 4; ++i) {
    assert(guarded[i] == 0xa5);
    assert(guarded[4 + input.size() + i] == 0xa5);
  }
  uint8_t onlyEl[] = {0, 0, 1, 0x7e, 1, 0xab};
  assert(wholphin::stripEl(onlyEl, sizeof(onlyEl)) == 0);
  assert(wholphin::stripEl(nullptr, 0) == 0);
  uint8_t truncated[] = {0, 0, 1};
  assert(wholphin::stripEl(truncated, sizeof(truncated)) == 0);
}
