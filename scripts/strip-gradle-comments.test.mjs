import assert from "node:assert/strict";
import test from "node:test";
import {
  findCompileOptionsBlock,
  stripGradleComments,
  validateCompileOptionsJava21,
} from "./strip-gradle-comments.mjs";

const validBlock = `
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_21
        targetCompatibility JavaVersion.VERSION_21
    }
}
`;

test("real compileOptions block passes validation", () => {
  const result = validateCompileOptionsJava21(validBlock);
  assert.equal(result.ok, true);
  assert.ok(findCompileOptionsBlock(validBlock));
});

test("block-commented compileOptions does not count", () => {
  const content = `
android {
    /*
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_21
        targetCompatibility JavaVersion.VERSION_21
    }
    */
}
`;
  assert.equal(findCompileOptionsBlock(content), null);
  assert.equal(validateCompileOptionsJava21(content).ok, false);
});

test("line-commented compileOptions does not count", () => {
  const content = `
android {
    // compileOptions {
    //     sourceCompatibility JavaVersion.VERSION_21
    //     targetCompatibility JavaVersion.VERSION_21
    // }
}
`;
  assert.equal(findCompileOptionsBlock(content), null);
  assert.equal(validateCompileOptionsJava21(content).ok, false);
});

test("comment markers inside strings are preserved", () => {
  const content = 'def x = "/* not a comment */";\n';
  assert.equal(stripGradleComments(content), content);
});

test("compileOptions text only inside a string does not count", () => {
  const content = `
android {
    def note = "compileOptions { sourceCompatibility JavaVersion.VERSION_21 targetCompatibility JavaVersion.VERSION_21 }"
}
`;
  assert.equal(findCompileOptionsBlock(content), null);
  assert.equal(validateCompileOptionsJava21(content).ok, false);
});

test("quoted note mentioning compatibility inside compileOptions does not pass", () => {
  const content = `
android {
    compileOptions {
        def note = "sourceCompatibility JavaVersion.VERSION_21 targetCompatibility JavaVersion.VERSION_21"
    }
}
`;
  assert.equal(validateCompileOptionsJava21(content).ok, false);
});

test("Groovy assignment syntax for compatibility is accepted", () => {
  const content = `
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
`;
  assert.equal(validateCompileOptionsJava21(content).ok, true);
});
