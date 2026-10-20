/**
 * Remove Gradle/Groovy line and block comments while preserving string literals.
 */

export function stripGradleComments(input) {
  let out = "";
  let i = 0;

  while (i < input.length) {
    const c = input[i];
    const next = input[i + 1];

    if (c === "/" && next === "/") {
      i += 2;
      while (i < input.length && input[i] !== "\n") {
        out += " ";
        i++;
      }
      continue;
    }

    if (c === "/" && next === "*") {
      i += 2;
      while (i < input.length) {
        if (input[i] === "*" && input[i + 1] === "/") {
          out += "  ";
          i += 2;
          break;
        }
        out += input[i] === "\n" ? "\n" : " ";
        i++;
      }
      continue;
    }

    if (c === '"') {
      const chunk = readQuoted(input, i, '"');
      out += chunk.text;
      i = chunk.end;
      continue;
    }

    if (c === "'") {
      const chunk = readQuoted(input, i, "'");
      out += chunk.text;
      i = chunk.end;
      continue;
    }

    out += c;
    i++;
  }

  return out;
}

function readQuoted(input, start, quote) {
  if (
    input[start + 1] === quote &&
    input[start + 2] === quote
  ) {
    let i = start + 3;
    let text = quote + quote + quote;
    while (i < input.length) {
      if (
        input[i] === quote &&
        input[i + 1] === quote &&
        input[i + 2] === quote
      ) {
        text += quote + quote + quote;
        return { text, end: i + 3 };
      }
      text += input[i];
      i++;
    }
    return { text, end: i };
  }

  let i = start + 1;
  let text = quote;
  while (i < input.length) {
    text += input[i];
    if (input[i] === "\\") {
      i++;
      if (i < input.length) {
        text += input[i];
        i++;
      }
      continue;
    }
    if (input[i] === quote) {
      return { text, end: i + 1 };
    }
    i++;
  }
  return { text, end: i };
}

export function findCompileOptionsBlock(content) {
  const stripped = stripGradleComments(content);
  const match = stripped.match(/compileOptions\s*\{[\s\S]*?\}/);
  return match ? match[0] : null;
}

export function validateCompileOptionsJava21(content) {
  const block = findCompileOptionsBlock(content);
  if (!block) {
    return { ok: false, reason: "missing compileOptions block" };
  }
  const hasSource = /sourceCompatibility\s+JavaVersion\.VERSION_21/.test(block);
  const hasTarget = /targetCompatibility\s+JavaVersion\.VERSION_21/.test(block);
  if (!hasSource || !hasTarget) {
    return {
      ok: false,
      reason:
        "compileOptions must set sourceCompatibility and targetCompatibility to JavaVersion.VERSION_21",
    };
  }
  return { ok: true };
}
