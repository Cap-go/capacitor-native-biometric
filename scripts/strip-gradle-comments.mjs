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

function maskQuotedStrings(input) {
  let out = "";
  let i = 0;

  while (i < input.length) {
    if (input[i] === '"' || input[i] === "'") {
      const chunk = readQuoted(input, i, input[i]);
      out += chunk.text.replace(/[^\n]/g, " ");
      i = chunk.end;
      continue;
    }
    out += input[i];
    i++;
  }

  return out;
}

export function findCompileOptionsBlock(content) {
  const stripped = stripGradleComments(content);
  const searchable = maskQuotedStrings(stripped);
  const match = searchable.match(/compileOptions\s*\{[\s\S]*?\}/);
  if (!match) {
    return null;
  }
  const blockStart = match.index ?? 0;
  const blockEnd = blockStart + match[0].length;
  return stripped.slice(blockStart, blockEnd);
}

export function validateCompileOptionsJava21(content) {
  const block = findCompileOptionsBlock(content);
  if (!block) {
    return { ok: false, reason: "missing compileOptions block" };
  }
  const masked = maskQuotedStrings(block);
  const hasSource = /sourceCompatibility\s*=?\s+JavaVersion\.VERSION_21/.test(
    masked,
  );
  const hasTarget = /targetCompatibility\s*=?\s+JavaVersion\.VERSION_21/.test(
    masked,
  );
  if (!hasSource || !hasTarget) {
    return {
      ok: false,
      reason:
        "compileOptions must set sourceCompatibility and targetCompatibility to JavaVersion.VERSION_21",
    };
  }
  return { ok: true };
}
